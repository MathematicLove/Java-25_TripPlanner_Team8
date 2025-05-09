package org.tripplanner.modules.triphelper;

import org.springframework.stereotype.Component;
import org.tripplanner.domain.Trip;

import reactor.core.publisher.Mono;

@Component
public class TripHelperController {

    private final TripHelperService service;

    public TripHelperController(TripHelperService service) {
        this.service = service;
    }

    public Mono<String> handleShowOngoingTrip(Long chatId) {
        return service.getOngoingTrips(chatId)
                .collectList()
                .map(trips -> {
                    if (trips.isEmpty()) return "У вас нет активной поездки.";
                    Trip trip = trips.get(0); // предполагается 1 активная поездка
                    return "Активная поездка:\n" +
                            trip.getName() + " (" +
                            trip.getStartDate() + " — " + trip.getEndDate() + ")";
                });
    }

    public Mono<String> handleAddNote(String pointId, String note) {
        return service.addNoteToPoint(pointId, note)
                .map(p -> "Заметка добавлена к точке \"" + p.getName() + "\".");
    }

    public Mono<String> handleMarkPoint(String pointId) {
        return service.markPointVisited(pointId)
                .map(p -> "Точка \"" + p.getName() + "\" отмечена как посещённая.");
    }
}
