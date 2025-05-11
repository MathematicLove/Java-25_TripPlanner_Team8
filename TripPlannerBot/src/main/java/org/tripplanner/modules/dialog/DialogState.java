package org.tripplanner.modules.dialog;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class DialogState {
    public enum Command {
        PLAN_TRIP,
        ADD_POINT,
        ADD_ROUTE,
        FINISH_PLANNING,
        ADD_NOTE,
        MARK_POINT,
        RATE_FINISHED,
        DELETE_PLANNED,
        SET_ONGOING
    }

    public enum Step {
        WAITING_NAME,
        WAITING_START_DATE,
        WAITING_END_DATE,
        WAITING_POINT_NAME,
        WAITING_LATITUDE,
        WAITING_LONGITUDE,
        WAITING_TRIP_NAME,
        WAITING_ROUTE_DATE,
        WAITING_NOTE,
        WAITING_RATING,
        WAITING_LOCATION
    }

    public static class CommandState {
        public final Command command;
        public Step currentStep;
        private final Map<String, Object> data;

        public CommandState(Command command) {
            this.command = command;
            this.currentStep = getInitialStep(command);
            this.data = new HashMap<>();
        }

        private Step getInitialStep(Command command) {
            return switch (command) {
                case PLAN_TRIP -> Step.WAITING_NAME;
                case ADD_POINT -> Step.WAITING_TRIP_NAME;
                case ADD_ROUTE -> Step.WAITING_TRIP_NAME;
                case FINISH_PLANNING -> Step.WAITING_TRIP_NAME;
                case ADD_NOTE -> Step.WAITING_TRIP_NAME;
                case MARK_POINT -> Step.WAITING_TRIP_NAME;
                case RATE_FINISHED -> Step.WAITING_TRIP_NAME;
                case DELETE_PLANNED -> Step.WAITING_TRIP_NAME;
                case SET_ONGOING -> Step.WAITING_TRIP_NAME;
            };
        }
    }

    private final Map<Long, CommandState> states = new HashMap<>();
    private static final Pattern LATIN_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s]+$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern COORDINATES_PATTERN = Pattern.compile("^(-?\\d+(\\.\\d+)?),\\s*(-?\\d+(\\.\\d+)?)$");

    public void startDialog(Long chatId, Command command) {
        states.put(chatId, new CommandState(command));
    }

    public void endDialog(Long chatId) {
        states.remove(chatId);
    }

    public boolean isInDialog(Long chatId) {
        return states.containsKey(chatId);
    }

    public CommandState getState(Long chatId) {
        return states.get(chatId);
    }

    public void setData(Long chatId, String key, Object value) {
        CommandState state = states.get(chatId);
        if (state != null) {
            state.data.put(key, value);
        }
    }

    public Object getData(Long chatId, String key) {
        CommandState state = states.get(chatId);
        return state != null ? state.data.get(key) : null;
    }

    public void nextStep(Long chatId) {
        CommandState state = states.get(chatId);
        if (state != null) {
            state.currentStep = getNextStep(state.command, state.currentStep);
        }
    }

    public Step getNextStep(Command command, Step currentStep) {
        return switch (command) {
            case PLAN_TRIP -> switch (currentStep) {
                case WAITING_NAME -> Step.WAITING_START_DATE;
                case WAITING_START_DATE -> Step.WAITING_END_DATE;
                case WAITING_END_DATE -> null;
                default -> null;
            };
            case ADD_POINT -> switch (currentStep) {
                case WAITING_TRIP_NAME -> Step.WAITING_POINT_NAME;
                case WAITING_POINT_NAME -> Step.WAITING_LATITUDE;
                case WAITING_LATITUDE -> Step.WAITING_LONGITUDE;
                case WAITING_LONGITUDE -> null;
                default -> null;
            };
            case ADD_ROUTE -> switch (currentStep) {
                case WAITING_TRIP_NAME -> Step.WAITING_POINT_NAME;
                case WAITING_POINT_NAME -> Step.WAITING_ROUTE_DATE;
                case WAITING_ROUTE_DATE -> null;
                default -> null;
            };
            case FINISH_PLANNING -> switch (currentStep) {
                case WAITING_TRIP_NAME -> null;
                default -> null;
            };
            case ADD_NOTE -> switch (currentStep) {
                case WAITING_TRIP_NAME -> Step.WAITING_NOTE;
                case WAITING_NOTE -> null;
                default -> null;
            };
            case MARK_POINT -> switch (currentStep) {
                case WAITING_TRIP_NAME -> Step.WAITING_POINT_NAME;
                case WAITING_POINT_NAME -> null;
                default -> null;
            };
            case RATE_FINISHED -> switch (currentStep) {
                case WAITING_TRIP_NAME -> Step.WAITING_RATING;
                case WAITING_RATING -> null;
                default -> null;
            };
            case DELETE_PLANNED -> switch (currentStep) {
                case WAITING_TRIP_NAME -> null;
                default -> null;
            };
            case SET_ONGOING -> switch (currentStep) {
                case WAITING_TRIP_NAME -> Step.WAITING_LOCATION;
                case WAITING_LOCATION -> null;
                default -> null;
            };
        };
    }

    public String getPrompt(Long chatId) {
        CommandState state = states.get(chatId);
        if (state == null) return null;

        return switch (state.currentStep) {
            case WAITING_NAME -> "Как Вы хотите назвать поездку? (только латиница)";
            case WAITING_START_DATE -> "Когда Вы планируете начать поездку? (Ввод YYYY-MM-DD)";
            case WAITING_END_DATE -> "Когда Вы планируете завершить поездку? (Ввод YYYY-MM-DD)";
            case WAITING_TRIP_NAME -> "Введите название поездки:";
            case WAITING_POINT_NAME -> "Введите название точки (только латиница):";
            case WAITING_LATITUDE -> "Введите широту (-90 до 90):";
            case WAITING_LONGITUDE -> "Введите долготу (-180 до 180):";
            case WAITING_ROUTE_DATE -> "Введите дату маршрута (формат: YYYY-MM-DD):";
            case WAITING_NOTE -> "Введите заметку:";
            case WAITING_RATING -> "Введите оценку (от 1 до 5):";
            case WAITING_LOCATION -> "Отправьте свою геопозицию:";
        };
    }

    public String validateInput(Long chatId, String input) {
        CommandState state = states.get(chatId);
        if (state == null) return null;

        return switch (state.currentStep) {
            case WAITING_NAME -> validateName(input);
            case WAITING_START_DATE -> validateDate(input);
            case WAITING_END_DATE -> validateDate(input);
            case WAITING_TRIP_NAME -> !input.trim().isEmpty() ? null : "Название поездки не может быть пустым";
            case WAITING_POINT_NAME -> validateName(input);
            case WAITING_LATITUDE -> validateLatitude(input);
            case WAITING_LONGITUDE -> validateLongitude(input);
            case WAITING_NOTE -> !input.trim().isEmpty() ? null : "Заметка не может быть пустой";
            case WAITING_RATING -> {
                try {
                    int rating = Integer.parseInt(input);
                    if (rating < 1 || rating > 5) {
                        yield "Ошибка: оценка должна быть от 1 до 5";
                    }
                    yield null;
                } catch (NumberFormatException e) {
                    yield "Ошибка: введите число от 1 до 5";
                }
            }
            case WAITING_LOCATION -> null;
            case WAITING_ROUTE_DATE -> validateDate(input);
        };
    }

    private String validateName(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Название не может быть пустым";
        }
        if (!input.matches("[a-zA-Z0-9\\s]+")) {
            return "Название должно содержать только латинские буквы, цифры и пробелы";
        }
        return null;
    }

    private String validateDate(String input) {
        try {
            LocalDate.parse(input, DATE_FORMATTER);
            return null;
        } catch (DateTimeParseException e) {
            return "Неверный формат даты. Используйте формат YYYY-MM-DD";
        }
    }

    private String validateLatitude(String input) {
        try {
            double lat = Double.parseDouble(input);
            if (lat < -90 || lat > 90) {
                return "Широта должна быть от -90 до 90 градусов";
            }
            return null;
        } catch (NumberFormatException e) {
            return "Введите корректное число";
        }
    }

    private String validateLongitude(String input) {
        try {
            double lon = Double.parseDouble(input);
            if (lon < -180 || lon > 180) {
                return "Долгота должна быть от -180 до 180 градусов";
            }
            return null;
        } catch (NumberFormatException e) {
            return "Введите корректное число";
        }
    }

    private String validateCoordinates(String input) {
        String[] parts = input.split(",");
        if (parts.length != 2) {
            return "Введите координаты в формате 'широта, долгота'";
        }
        String latError = validateLatitude(parts[0].trim());
        if (latError != null) {
            return latError;
        }
        return validateLongitude(parts[1].trim());
    }

    public String getErrorMessage(Long chatId) {
        CommandState state = states.get(chatId);
        if (state == null) return "Ошибка: диалог не найден";

        return switch (state.currentStep) {
            case WAITING_NAME, WAITING_POINT_NAME, WAITING_TRIP_NAME -> "Упс! Только латыница";
            case WAITING_START_DATE, WAITING_END_DATE, WAITING_ROUTE_DATE -> "Упс! Указанное время прошло! Оно летит быстро :(";
            case WAITING_LATITUDE -> "Широта должна быть числом от -90 до 90";
            case WAITING_LONGITUDE -> "Долгота должна быть числом от -180 до 180";
            case WAITING_NOTE -> "Заметка не может быть пустой";
            case WAITING_RATING -> "Оценка должна быть числом от 1 до 5";
            case WAITING_LOCATION -> "Пожалуйста, отправьте геопозицию, используя кнопку 'Отправить геопозицию'";
        };
    }
} 