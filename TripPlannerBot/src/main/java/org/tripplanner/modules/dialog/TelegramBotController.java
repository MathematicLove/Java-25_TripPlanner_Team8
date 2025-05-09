package org.tripplanner.modules.dialog;

import org.springframework.stereotype.Component;
import org.tripplanner.modules.plannedtrips.PlannedTripsController;
import org.tripplanner.modules.triphelper.TripHelperController;
import org.tripplanner.modules.triphistory.TripHistoryController;

import reactor.core.publisher.Mono;

@Component
public class TelegramBotController {

    private final PlannedTripsController plannedTrips;
    private final TripHelperController tripHelper;
    private final TripHistoryController tripHistory;
    private final DialogState dialogState;

    public TelegramBotController(PlannedTripsController plannedTrips,
                               TripHelperController tripHelper,
                               TripHistoryController tripHistory) {
        this.plannedTrips = plannedTrips;
        this.tripHelper = tripHelper;
        this.tripHistory = tripHistory;
        this.dialogState = new DialogState();
    }

    public Mono<String> handleCommand(Long chatId, String messageText) {
        // Если пользователь находится в диалоге, обрабатываем его ввод
        if (dialogState.isInDialog(chatId)) {
            return handleDialogInput(chatId, messageText);
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

            case "/setstartpoint", "/setstartingpoint" -> {
                dialogState.startDialog(chatId, DialogState.Command.SET_START_POINT);
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

            case "/cancelplanning" -> plannedTrips.handleCancelPlanning(chatId);

            case "/deleteplanned" -> {
                if (parts.length < 2) {
                    yield Mono.just("Формат: /deleteplanned <tripId>");
                }
                yield plannedTrips.handleDeletePlanned(chatId, parts[1]);
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
                    yield Mono.just("Формат: /finisheddetails <tripId>");
                }
                yield tripHistory.handleFinishedDetails(parts[1]);
            }

            case "/ratefinished" -> {
                dialogState.startDialog(chatId, DialogState.Command.RATE_FINISHED);
                yield Mono.just(dialogState.getPrompt(chatId));
            }

            case "/start" -> plannedTrips.handleStartCommand(chatId);

            case "/help" -> Mono.just(
                    "Доступные команды:\n" +
                            "\n📍 Планирование:\n" +
                            "/showplanned — показать запланированные поездки\n" +
                            "/plantrip — создать поездку\n" +
                            "/addpoint — добавить точку\n" +
                            "/setstartpoint или /setstartingpoint — установить начальную точку\n" +
                            "/addroute — добавить маршрут\n" +
                            "/finishplanning — завершить планирование\n" +
                            "/cancelplanning — отменить планирование\n" +
                            "/deleteplanned <tripId> — удалить поездку\n" +
                            "\n🗺 Помощник в поездке:\n" +
                            "/showongoingtrip — текущая поездка\n" +
                            "/addnote — добавить заметку к точке\n" +
                            "/markpoint — отметить точку посещённой\n" +
                            "\n📖 История:\n" +
                            "/triphistory — завершённые поездки\n" +
                            "/finisheddetails <tripId> — подробности поездки\n" +
                            "/ratefinished — оценить поездку"
            );

            default -> Mono.just("Неизвестная команда: " + command);
        };
    }

    private Mono<String> handleDialogInput(Long chatId, String input) {
        DialogState.CommandState state = dialogState.getState(chatId);
        if (state == null) {
            return Mono.just("Ошибка: диалог не найден");
        }

        // Проверяем валидность ввода
        if (!dialogState.validateInput(chatId, input)) {
            return Mono.just(dialogState.getErrorMessage(chatId));
        }

        // Сохраняем введенные данные
        switch (state.currentStep) {
            case WAITING_NAME -> dialogState.setData(chatId, "name", input);
            case WAITING_START_DATE -> dialogState.setData(chatId, "startDate", input);
            case WAITING_END_DATE -> dialogState.setData(chatId, "endDate", input);
            case WAITING_POINT_NAME -> dialogState.setData(chatId, "pointName", input);
            case WAITING_LATITUDE -> dialogState.setData(chatId, "latitude", input);
            case WAITING_LONGITUDE -> dialogState.setData(chatId, "longitude", input);
            case WAITING_TRIP_NAME -> dialogState.setData(chatId, "tripName", input);
            case WAITING_POINT_ID -> dialogState.setData(chatId, "pointId", input);
            case WAITING_ROUTE_DATE -> dialogState.setData(chatId, "routeDate", input);
            case WAITING_NOTE -> dialogState.setData(chatId, "note", input);
            case WAITING_RATING -> dialogState.setData(chatId, "rating", input);
            case WAITING_POINT_COORDINATES -> dialogState.setData(chatId, "coordinates", input);
        }

        // Переходим к следующему шагу
        dialogState.nextStep(chatId);

        // Получаем следующий шаг
        DialogState.CommandState nextState = dialogState.getState(chatId);
        if (nextState == null || nextState.currentStep == null) {
            // Если следующего шага нет, выполняем команду и завершаем диалог
            return executeCommand(chatId, state.command)
                    .doFinally(signalType -> dialogState.endDialog(chatId));
        }

        // Иначе возвращаем следующий вопрос
        return Mono.just(dialogState.getPrompt(chatId));
    }

    private Mono<String> executeCommand(Long chatId, DialogState.Command command) {
        return switch (command) {
            case PLAN_TRIP -> {
                String name = (String) dialogState.getData(chatId, "name");
                String startDate = (String) dialogState.getData(chatId, "startDate");
                String endDate = (String) dialogState.getData(chatId, "endDate");
                yield plannedTrips.handlePlanTrip(chatId, name, startDate, endDate);
            }
            case ADD_POINT -> {
                String pointName = (String) dialogState.getData(chatId, "pointName");
                double latitude = Double.parseDouble((String) dialogState.getData(chatId, "latitude"));
                double longitude = Double.parseDouble((String) dialogState.getData(chatId, "longitude"));
                yield plannedTrips.handleAddPoint(String.valueOf(chatId), pointName, latitude, longitude);
            }
            case SET_START_POINT -> {
                String tripName = (String) dialogState.getData(chatId, "tripName");
                String coordinates = (String) dialogState.getData(chatId, "coordinates");
                String[] parts = coordinates.split(",");
                double latitude = Double.parseDouble(parts[0].trim());
                double longitude = Double.parseDouble(parts[1].trim());
                yield plannedTrips.handleSetStartPoint(tripName, latitude, longitude)
                        .map(trip -> "✅ Стартовая точка успешно добавлена!");
            }
            case ADD_ROUTE -> {
                String tripName = (String) dialogState.getData(chatId, "tripName");
                String pointId = (String) dialogState.getData(chatId, "pointId");
                String routeDate = (String) dialogState.getData(chatId, "routeDate");
                yield plannedTrips.handleAddRoute(tripName, pointId, routeDate);
            }
            case FINISH_PLANNING -> {
                String tripName = (String) dialogState.getData(chatId, "tripName");
                yield plannedTrips.handleFinishPlanning(chatId)
                        .then(Mono.just("Рад что Вы отдохнули! Если желаете напишите заметку о своем путешествии с помощью /addnote"));
            }
            case ADD_NOTE -> {
                String pointId = (String) dialogState.getData(chatId, "pointId");
                String note = (String) dialogState.getData(chatId, "note");
                yield tripHelper.handleAddNote(pointId, note);
            }
            case MARK_POINT -> {
                String pointId = (String) dialogState.getData(chatId, "pointId");
                yield tripHelper.handleMarkPoint(pointId);
            }
            case RATE_FINISHED -> {
                String tripName = (String) dialogState.getData(chatId, "tripName");
                int rating = Integer.parseInt((String) dialogState.getData(chatId, "rating"));
                yield tripHistory.handleRateFinished(tripName, rating);
            }
        };
    }
}
