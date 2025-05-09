package org.tripplanner.modules.triphistory;

import org.springframework.stereotype.Component;
import org.tripplanner.domain.Trip;

import reactor.core.publisher.Mono;

@Component
public class TripHistoryController {

    private final TripHistoryService service;

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

    public Mono<String> handleFinishedDetails(String tripId) {
        return service.getFinishedTrip(tripId)
                .map(trip -> "Подробности поездки:\n" +
                        "Название: " + trip.getName() + "\n" +
                        "Даты: " + trip.getStartDate() + " — " + trip.getEndDate() + "\n" +
                        "Оценка: " + (trip.getRating() != null ? trip.getRating() : "не установлена"));
    }

    public Mono<String> handleRateFinished(String tripId, int rating) {
        return service.setFinishedTripRating(tripId, rating)
                .map(trip -> "Оценка " + rating + " установлена для поездки \"" + trip.getName() + "\".");
    }
}
