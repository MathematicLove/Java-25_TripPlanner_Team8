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
                    StringBuilder sb = new StringBuilder("Активные поездки:\n");
                    for (Trip trip : trips) {
                        if (trip.getEndDate().isAfter(java.time.LocalDate.now()) && 
                            !trip.getStartDate().isAfter(java.time.LocalDate.now())) {
                            sb.append("• ").append(trip.getName())
                                    .append(" (").append(trip.getStartDate())
                                    .append(" — ").append(trip.getEndDate()).append(")\n");
                        }
                    }
                    String response = sb.toString();
                    return response.equals("Активные поездки:\n") ? "У вас нет активной поездки." : response;
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
