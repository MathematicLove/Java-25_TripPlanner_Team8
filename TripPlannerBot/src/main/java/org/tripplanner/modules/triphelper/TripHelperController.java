package org.tripplanner.modules.triphelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tripplanner.domain.Point;
import org.tripplanner.domain.Trip;

import reactor.core.publisher.Mono;

@Component
public class TripHelperController {

    private static final Logger logger = LoggerFactory.getLogger(TripHelperController.class);

    private final TripHelperService service;

    public TripHelperController(TripHelperService service) {
        this.service = service;
    }

    public Mono<String> handleShowOngoingTrip(Long chatId) {
        logger.info("Запрос на показ активных поездок для пользователя {}", chatId);

        return service.getOngoingTrips(chatId)
                .collectList()
                .map(trips -> {
                    if (trips.isEmpty()) {
                        logger.info("Активных поездок не найдено для пользователя {}", chatId);
                        return "У вас нет активных поездок на сегодня.";
                    }

                    StringBuilder sb = new StringBuilder("Активные поездки на сегодня:\n");
                    for (Trip trip : trips) {
                        sb.append("• ").append(trip.getName())
                                .append(" (").append(trip.getStartDate())
                                .append(" — ").append(trip.getEndDate()).append(")\n");
                    }
                    String response = sb.toString();
                    logger.info("Найдены активные поездки для пользователя {}: {}", chatId, response);
                    return response;
                })
                .onErrorResume(e -> {
                    logger.error("Ошибка при получении активных поездок для пользователя {}: {}", 
                        chatId, e.getMessage());
                    return Mono.just("Произошла ошибка при получении списка поездок. Попробуйте позже.");
                });
    }

    public Mono<String> handleAddNote(Long chatId, String tripName, String note) {
        return service.addNote(chatId, tripName, note)
                .onErrorResume(e -> Mono.just(e.getMessage()));
    }

    public Mono<String> handleMarkPoint(Long chatId, String tripName, String pointName) {
        return service.markPointVisited(chatId, tripName, pointName)
                .map(p -> "Точка \"" + p.getName() + "\" отмечена как посещённая.")
                .onErrorResume(e -> Mono.just("Ошибка: " + e.getMessage()));
    }

    public Mono<Void> handleLocationUpdate(Long chatId, double latitude, double longitude) {
        logger.info("Получена геопозиция от пользователя {}: широта={}, долгота={}", 
            chatId, latitude, longitude);
        return service.updateUserLocation(chatId, latitude, longitude);
    }

    public Mono<Point> markPointVisited(Long chatId, String tripName, String pointName) {
        return service.markPointVisited(chatId, tripName, pointName);
    }
}
