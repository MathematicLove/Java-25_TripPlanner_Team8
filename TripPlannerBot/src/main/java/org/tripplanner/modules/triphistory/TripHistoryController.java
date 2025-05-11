package org.tripplanner.modules.triphistory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tripplanner.domain.Point;
import org.tripplanner.domain.Trip;

import reactor.core.publisher.Mono;

@Component
public class TripHistoryController {

    private final TripHistoryService service;
    private static final Logger logger = LoggerFactory.getLogger(TripHistoryController.class);

    public TripHistoryController(TripHistoryService service) {
        this.service = service;
    }

    public Mono<String> handleTripHistory(Long chatId) {
        return service.getFinishedTrips(chatId)
                .collectList()
                .map(trips -> {
                    if (trips.isEmpty()) return "У вас нет завершённых поездок.";
                    StringBuilder sb = new StringBuilder("Завершённые поездки:\n");
                    for (Trip trip : trips) {
                        sb.append("• ").append(trip.getName())
                                .append(" (").append(trip.getStartDate())
                                .append(" — ").append(trip.getEndDate()).append(")\n");
                    }
                    return sb.toString();
                });
    }

    public Mono<String> handleFinishedDetails(Long chatId, String tripId) {
        if (tripId == null || tripId.trim().isEmpty()) {
            return service.getAllFinishedTrips(chatId)
                    .collectList()
                    .map(trips -> {
                        if (trips.isEmpty()) return "У вас нет завершённых поездок.";
                        StringBuilder sb = new StringBuilder("Подробности завершённых поездок:\n\n");
                        for (Trip trip : trips) {
                            sb.append("Поездка: ").append(trip.getName())
                                    .append("\nДаты: ").append(trip.getStartDate())
                                    .append(" — ").append(trip.getEndDate())
                                    .append("\nОценка: ").append(trip.getRating() != null ? trip.getRating() + " ⭐" : "не установлена")
                                    .append("\nЗаметки: ");
                            if (trip.getNotes() != null && !trip.getNotes().isEmpty()) {
                                for (String note : trip.getNotes()) {
                                    sb.append("\n- ").append(note);
                                }
                            } else {
                                sb.append("нет");
                            }
                            sb.append("\n\nПосещённые точки:\n");
                            if (trip.getPoints() != null && !trip.getPoints().isEmpty()) {
                                for (Point point : trip.getPoints()) {
                                    if (point.isVisited()) {
                                        sb.append("• ").append(point.getName());
                                        if (point.getNotes() != null && !point.getNotes().isEmpty()) {
                                            sb.append(" (Заметки: ");
                                            for (String note : point.getNotes()) {
                                                sb.append("\n  - ").append(note);
                                            }
                                            sb.append(")");
                                        }
                                        sb.append("\n");
                                    }
                                }
                            } else {
                                sb.append("нет посещённых точек");
                            }
                            sb.append("\n");
                        }
                        return sb.toString();
                    });
        }

        return service.getFinishedTrip(tripId)
                .flatMap(trip -> service.getAllPoints(trip.getId())
                        .collectList()
                        .map(points -> {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Подробности поездки:\n")
                                    .append("Название: ").append(trip.getName()).append("\n")
                                    .append("Даты: ").append(trip.getStartDate())
                                    .append(" — ").append(trip.getEndDate()).append("\n")
                                    .append("Оценка: ").append(trip.getRating() != null ? trip.getRating() + " ⭐" : "не установлена").append("\n")
                                    .append("Заметки: ");
                            if (trip.getNotes() != null && !trip.getNotes().isEmpty()) {
                                for (String note : trip.getNotes()) {
                                    sb.append("\n- ").append(note);
                                }
                            } else {
                                sb.append("нет");
                            }
                            sb.append("\n\nПосещённые точки:\n");
                            if (!points.isEmpty()) {
                                for (Point point : points) {
                                    if (point.isVisited()) {
                                        sb.append("• ").append(point.getName());
                                        if (point.getNotes() != null && !point.getNotes().isEmpty()) {
                                            sb.append(" (Заметки: ");
                                            for (String note : point.getNotes()) {
                                                sb.append("\n  - ").append(note);
                                            }
                                            sb.append(")");
                                        }
                                        sb.append("\n");
                                    }
                                }
                            } else {
                                sb.append("нет посещённых точек");
                            }
                            return sb.toString();
                        }));
    }

    public Mono<String> handleRateFinished(Long chatId, String tripName, int rating) {
        logger.info("Handling rate finished request for trip {} with rating {}", tripName, rating);
        
        if (rating < 1 || rating > 5) {
            logger.warn("Invalid rating {} for trip {}", rating, tripName);
            return Mono.just("Ошибка: оценка должна быть от 1 до 5");
        }
        
        return service.findFinishedTripByName(chatId, tripName)
                .flatMap(trip -> {
                    logger.info("Found trip {} (ID: {}) for rating", tripName, trip.getId());
                    return service.setFinishedTripRating(trip.getId(), rating)
                            .map(t -> "Спасибо за оценку! Ваша поездка \"" + tripName + "\" оценена на " + rating + " ⭐");
                })
                .switchIfEmpty(Mono.just("Ошибка: поездка \"" + tripName + "\" не найдена в завершённых поездках. Убедитесь, что поездка была завершена с помощью /finishplanning"))
                .doOnError(e -> logger.error("Error rating trip {}: {}", tripName, e.getMessage()));
    }
}
