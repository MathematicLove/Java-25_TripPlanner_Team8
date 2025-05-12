package org.tripplanner.modules.dialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tripplanner.modules.plannedtrips.PlannedTripsController;
import org.tripplanner.modules.triphelper.TripHelperController;
import org.tripplanner.modules.triphelper.TripHelperService;
import org.tripplanner.modules.triphistory.TripHistoryController;
import org.tripplanner.repositories.UserDAO;

import reactor.core.publisher.Mono;

@Component
public class TelegramBotController {

    private final PlannedTripsController plannedTrips;
    private final TripHelperController tripHelper;
    private final TripHistoryController tripHistory;
    private final TripHelperService tripHelperService;
    private final DialogState dialogState;
    private final UserDAO userDAO;
    private static final Logger logger = LoggerFactory.getLogger(TelegramBotController.class);

    public TelegramBotController(PlannedTripsController plannedTrips,
                               TripHelperController tripHelper,
                               TripHistoryController tripHistory,
                               TripHelperService tripHelperService,
                               DialogState dialogState,
                               UserDAO userDAO) {
        this.plannedTrips = plannedTrips;
        this.tripHelper = tripHelper;
        this.tripHistory = tripHistory;
        this.tripHelperService = tripHelperService;
        this.dialogState = dialogState;
        this.userDAO = userDAO;
    }

    public Mono<String> handleCommand(Long chatId, String messageText) {
        // Если пользователь находится в диалоге, обрабатываем его ввод
        if (dialogState.isInDialog(chatId)) {
            DialogState.CommandState state = dialogState.getState(chatId);
            // Если текущий шаг - WAITING_LOCATION, то это не диалог, а фоновый процесс
            if (state != null && state.currentStep == DialogState.Step.WAITING_LOCATION) {
                dialogState.endDialog(chatId);
            } else {
                return handleDialogInput(chatId, messageText);
            }
        }

        // Обработка команд
        String[] parts = messageText.trim().split("\\s+");
        String command = parts[0].toLowerCase();

        return switch (command) {
            case "/showplanned" -> plannedTrips.handleShowPlanned(chatId);

            case "/plantrip" -> {
                dialogState.startDialog(chatId, DialogState.Command.PLAN_TRIP);
                yield Mono.just(dialogState.getPrompt(chatId));
            }

            case "/addpoint" -> {
                dialogState.startDialog(chatId, DialogState.Command.ADD_POINT);
                yield Mono.just(dialogState.getPrompt(chatId));
            }

            case "/addroute" -> {
                dialogState.startDialog(chatId, DialogState.Command.ADD_ROUTE);
                yield Mono.just(dialogState.getPrompt(chatId));
            }

            case "/finishplanning" -> {
                dialogState.startDialog(chatId, DialogState.Command.FINISH_PLANNING);
                yield Mono.just(dialogState.getPrompt(chatId));
            }

            case "/deleteplanned" -> {
                dialogState.startDialog(chatId, DialogState.Command.DELETE_PLANNED);
                yield Mono.just(dialogState.getPrompt(chatId));
            }

            case "/showongoingtrip" -> tripHelper.handleShowOngoingTrip(chatId);

            case "/addnote" -> {
                dialogState.startDialog(chatId, DialogState.Command.ADD_NOTE);
                yield Mono.just(dialogState.getPrompt(chatId));
            }

            case "/markpoint" -> {
                dialogState.startDialog(chatId, DialogState.Command.MARK_POINT);
                yield Mono.just(dialogState.getPrompt(chatId));
            }

            case "/triphistory" -> tripHistory.handleTripHistory(chatId);

            case "/finisheddetails" -> {
                if (parts.length < 2) {
                    yield tripHistory.handleFinishedDetails(chatId, null);
                }
                yield tripHistory.handleFinishedDetails(chatId, parts[1]);
            }

            case "/ratefinished" -> {
                dialogState.startDialog(chatId, DialogState.Command.RATE_FINISHED);
                yield Mono.just(dialogState.getPrompt(chatId));
            }

            case "/setongoing" -> {
                dialogState.startDialog(chatId, DialogState.Command.SET_ONGOING);
                yield Mono.just(dialogState.getPrompt(chatId));
            }

            case "/startontrip" -> {
                dialogState.startDialog(chatId, DialogState.Command.SET_ONGOING);
                yield plannedTrips.handleFinishPlanning(chatId)
                    .flatMap(response -> {
                        if (response.contains("нет запланированных поездок")) {
                            return Mono.just("У вас нет запланированных поездок. Сначала создайте поездку с помощью /plantrip");
                        }
                        return Mono.just("Выберите поездку из списка выше, чтобы начать отслеживание геопозиции. После выбора поездки отправьте свою геопозицию.");
                    });
            }

            case "/start" -> plannedTrips.handleStartCommand(chatId);

            case "/help" -> Mono.just(
                    "Доступные команды:\n" +
                            "\n📍 Планирование:\n" +
                            "/showplanned — показать запланированные поездки\n" +
                            "/plantrip — создать поездку\n" +
                            "/addpoint — добавить точку\n" +
                            "/addroute — добавить маршрут\n" +
                            "/finishplanning — завершить планирование\n" +
                            "/deleteplanned — удалить поездку\n" +
                            "\n🗺 Помощник в поездке:\n" +
                            "/showongoingtrip — текущая поездка\n" +
                            "/addnote — добавить заметку к точке\n" +
                            "/markpoint — отметить точку посещённой\n" +
                            "/setongoing — начать отслеживание геопозиции\n" +
                            "\n📖 История:\n" +
                            "/triphistory — завершённые поездки\n" +
                            "/finisheddetails — подробности поездки\n" +
                            "/ratefinished — оценить поездку"
            );

            default -> Mono.just("Неизвестная команда: " + command);
        };
    }

    public Mono<String> handleDialogInput(Long chatId, String input) {
        DialogState.CommandState state = dialogState.getState(chatId);
        if (state == null) {
            return Mono.just("Нет активного диалога. Используйте команды для начала работы.");
        }

        // Validate input
        String validationError = dialogState.validateInput(chatId, input);
        if (validationError != null) {
            return Mono.just(validationError);
        }

        // Save input data
        if (state.currentStep == DialogState.Step.WAITING_RATING) {
            try {
                int rating = Integer.parseInt(input);
                if (rating < 1 || rating > 5) {
                    return Mono.just("Ошибка: оценка должна быть от 1 до 5");
                }
                dialogState.setData(chatId, "rating", rating);
            } catch (NumberFormatException e) {
                return Mono.just("Ошибка: введите число от 1 до 5");
            }
        } else if (state.currentStep == DialogState.Step.WAITING_TRIP_NAME) {
            dialogState.setData(chatId, "tripName", input);
        } else if (state.currentStep == DialogState.Step.WAITING_NOTE) {
            dialogState.setData(chatId, "note", input);
        } else {
            String key;
            switch (state.currentStep) {
                case WAITING_NAME -> key = "name";
                case WAITING_START_DATE -> key = "startDate";
                case WAITING_END_DATE -> key = "endDate";
                case WAITING_LATITUDE -> key = "latitude";
                case WAITING_LONGITUDE -> key = "longitude";
                case WAITING_POINT_NAME -> key = "pointName";
                case WAITING_ROUTE_DATE -> key = "routeDate";
                default -> key = state.currentStep.toString().toLowerCase();
            }
            dialogState.setData(chatId, key, input);
        }

        // Get next step
        DialogState.Step nextStep = dialogState.getNextStep(state.command, state.currentStep);

        if (nextStep == null) {
            // Если следующего шага нет, выполняем команду и завершаем диалог
            return executeCommand(chatId, state.command, input)
                    .doFinally(signalType -> dialogState.endDialog(chatId));
        }

        // Обновляем текущий шаг
        state.currentStep = nextStep;

        // Иначе возвращаем следующий вопрос
        return Mono.just(dialogState.getPrompt(chatId));
    }

    private Mono<String> executeCommand(Long chatId, DialogState.Command command, String messageText) {
        return switch (command) {
            case PLAN_TRIP -> {
                String name = (String) dialogState.getData(chatId, "name");
                String startDate = (String) dialogState.getData(chatId, "startDate");
                String endDate = (String) dialogState.getData(chatId, "endDate");
                yield plannedTrips.handlePlanTrip(chatId, name, startDate, endDate);
            }
            case ADD_POINT -> {
                String tripName = (String) dialogState.getData(chatId, "tripName");
                String pointName = (String) dialogState.getData(chatId, "pointName");
                double latitude = Double.parseDouble((String) dialogState.getData(chatId, "latitude"));
                double longitude = Double.parseDouble((String) dialogState.getData(chatId, "longitude"));
                yield plannedTrips.handleAddPoint(chatId, tripName, pointName, latitude, longitude);
            }
            case ADD_ROUTE -> {
                String tripName = (String) dialogState.getData(chatId, "tripName");
                String pointName = (String) dialogState.getData(chatId, "pointName");
                String routeDate = (String) dialogState.getData(chatId, "routeDate");
                yield plannedTrips.handleAddRoute(chatId, tripName, pointName, routeDate);
            }
            case FINISH_PLANNING -> {
                String tripName = (String) dialogState.getData(chatId, "tripName");
                if (tripName == null) {
                    yield plannedTrips.handleFinishPlanning(chatId);
                } else {
                    yield plannedTrips.handleFinishPlanningWithName(chatId, tripName)
                            .then(Mono.just("Рад что Вы отдохнули! Если желаете напишите заметку о своем путешествии с помощью /addnote"));
                }
            }
            case ADD_NOTE -> {
                String tripName = (String) dialogState.getData(chatId, "tripName");
                String note = (String) dialogState.getData(chatId, "note");
                yield tripHelper.handleAddNote(chatId, tripName, note);
            }
            case MARK_POINT -> {
                String tripName = (String) dialogState.getData(chatId, "tripName");
                String pointName = (String) dialogState.getData(chatId, "pointName");
                if (tripName == null) {
                    dialogState.setData(chatId, "tripName", messageText);
                    yield Mono.just("Какую точку вы посетили?");
                } else if (pointName == null) {
                    dialogState.setData(chatId, "pointName", messageText);
                    yield tripHelperService.markPointVisited(chatId, tripName, messageText)
                            .map(point -> "Точка '" + point.getName() + "' отмечена как посещенная")
                            .onErrorResume(e -> {
                                if (e.getMessage().contains("Поездка с названием")) {
                                    return Mono.just("Упс! Не нашлось такой поездки. Для просмотра поездок: /showplanned");
                                } else if (e.getMessage().contains("Точка с названием")) {
                                    return Mono.just("Упс! Не нашлось такой точки. Если хотите создать точку: /addpoint");
                                }
                                return Mono.just("Ошибка: " + e.getMessage());
                            })
                            .doFinally(signalType -> dialogState.endDialog(chatId));
                } else {
                    yield tripHelperService.markPointVisited(chatId, tripName, pointName)
                            .map(point -> "Точка '" + point.getName() + "' отмечена как посещенная")
                            .onErrorResume(e -> {
                                if (e.getMessage().contains("Поездка с названием")) {
                                    return Mono.just("Упс! Не нашлось такой поездки. Для просмотра поездок: /showplanned");
                                } else if (e.getMessage().contains("Точка с названием")) {
                                    return Mono.just("Упс! Не нашлось такой точки. Если хотите создать точку: /addpoint");
                                }
                                return Mono.just("Ошибка: " + e.getMessage());
                            })
                            .doFinally(signalType -> dialogState.endDialog(chatId));
                }
            }
            case RATE_FINISHED -> {
                DialogState.CommandState commandState = dialogState.getState(chatId);
                if (commandState == null || commandState.currentStep == null) {
                    // Начало диалога - проверяем наличие завершённых поездок
                    yield tripHistory.handleTripHistory(chatId)
                            .flatMap(response -> {
                                if (response.contains("У вас пока нет завершённых поездок")) {
                                    return Mono.just(response);
                                }
                                return Mono.just("Введите название поездки из списка завершённых:");
                            });
                } else if (commandState.currentStep == DialogState.Step.WAITING_TRIP_NAME) {
                    String tripName = (String) dialogState.getData(chatId, "tripName");
                    if (tripName == null || tripName.isEmpty()) {
                        yield Mono.just("Ошибка: название поездки не может быть пустым");
                    }
                    dialogState.nextStep(chatId);
                    yield Mono.just("Введите оценку (от 1 до 5):");
                } else if (commandState.currentStep == DialogState.Step.WAITING_RATING) {
                    String tripName = (String) dialogState.getData(chatId, "tripName");
                    Object ratingObj = dialogState.getData(chatId, "rating");
                    
                    if (ratingObj == null) {
                        yield Mono.just("Ошибка: оценка не была введена");
                    }
                    
                    int rating = (Integer) ratingObj;
                    yield tripHistory.handleRateFinished(chatId, tripName, rating)
                            .doFinally(signalType -> dialogState.endDialog(chatId));
                }
                yield Mono.just("Неизвестная ошибка в процессе оценки поездки");
            }
            case DELETE_PLANNED -> {
                String tripName = (String) dialogState.getData(chatId, "tripName");
                yield plannedTrips.handleDeletePlanned(chatId, tripName);
            }
            case SET_ONGOING -> {
                String tripName = (String) dialogState.getData(chatId, "tripName");
                yield plannedTrips.handleFinishPlanningWithName(chatId, tripName)
                    .flatMap(trip -> {
                        if (trip == null) {
                            return Mono.just("Такой поездки нет! Если хотите создать поездку воспользуйтесь: /plantrip или просмотрите свои поездки с помощью: /showplanned");
                        }
                        return userDAO.setOngoingTrip(chatId, trip.getId())
                            .then(Mono.just("Поездка \"" + trip.getName() + "\" установлена как активная. Теперь вы можете делиться своей геопозицией, нажав на кнопку 'Отправить геопозицию'."));
                    })
                    .switchIfEmpty(Mono.just("Такой поездки нет! Если хотите создать поездку воспользуйтесь: /plantrip или просмотрите свои поездки с помощью: /showplanned"))
                    .doFinally(signalType -> dialogState.endDialog(chatId));
            }
            default -> Mono.just("Неизвестная команда");
        };
    }

    public Mono<String> handleLocation(Long chatId, double latitude, double longitude) {
        return tripHelper.handleLocationUpdate(chatId, latitude, longitude)
            .then(Mono.just("Геопозиция принята. Продолжайте делиться геопозицией для отслеживания точек маршрута."))
            .onErrorResume(e -> {
                logger.error("Error handling location update for chat {}: {}", chatId, e.getMessage());
                return Mono.just("Ошибка при обработке геопозиции. Пожалуйста, попробуйте снова.");
            })
            .doFinally(signalType -> {
                // Завершаем диалог после получения геопозиции
                dialogState.endDialog(chatId);
            });
    }
}
