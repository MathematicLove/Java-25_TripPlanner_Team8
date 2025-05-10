package org.tripplanner.modules.plannedtrips;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;
import org.tripplanner.domain.Trip;
import org.tripplanner.repositories.mongodb.TripDAOImpl;

import reactor.core.publisher.Mono;

@Component
public class PlannedTripsController {

    private final PlannedTripsService service;
    private final TripDAOImpl tripDAO;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public PlannedTripsController(PlannedTripsService service, TripDAOImpl tripDAO) {
        this.service = service;
        this.tripDAO = tripDAO;
    }

    public Mono<String> handleStartCommand(Long chatId) {
        return service.registerUserIfNeeded(chatId)
                .thenReturn("Привет! Добро пожаловать в TripPlanner ✈️\nВведи /help, чтобы посмотреть команды.");
    }

    public Mono<String> handleShowPlanned(Long chatId) {
        System.out.println("Handling show planned trips for chatId: " + chatId);
        
        return service.getAllPlannedTrips(chatId)
                .doOnNext(trip -> System.out.println("Received trip in controller: " + trip.getName() + " with ID: " + trip.getId() + 
                        ", dates: " + trip.getStartDate() + " - " + trip.getEndDate()))
                .collectList()
                .doOnNext(trips -> {
                    System.out.println("Collected " + trips.size() + " trips");
                    trips.forEach(trip -> System.out.println("Trip in list: " + trip.getName() + " with ID: " + trip.getId() + 
                            ", dates: " + trip.getStartDate() + " - " + trip.getEndDate()));
                })
                .flatMap(trips -> {
                    if (trips.isEmpty()) {
                        System.out.println("No trips to display");
                        return Mono.just("У вас нет запланированных поездок.");
                    }
                    StringBuilder sb = new StringBuilder("Запланированные поездки:\n");
                    for (Trip trip : trips) {
                        sb.append("• ").append(trip.getName())
                                .append(" (").append(trip.getStartDate())
                                .append(" — ").append(trip.getEndDate()).append(")\n");
                    }
                    String response = sb.toString();
                    System.out.println("Generated response: " + response);
                    return Mono.just(response);
                })
                .doOnError(e -> {
                    System.out.println("Error in handleShowPlanned: " + e.getMessage());
                    e.printStackTrace();
                });
    }

    public Mono<String> handlePlanTrip(Long chatId, String name, String startDate, String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
            LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);
            LocalDate today = LocalDate.now();

            if (start.isBefore(today)) {
                return Mono.just("Упс! Указанное время прошло! Оно летит быстро :(");
            }

            if (end.isBefore(today)) {
                return Mono.just("Упс! Указанное время прошло! Оно летит быстро :(");
            }

            if (start.isAfter(end)) {
                return Mono.just("Дата начала не может быть позже даты окончания");
            }

            return tripDAO.createTrip(chatId, name, start, end)
                    .map(trip -> "Поездка успешно создана!")
                    .onErrorResume(e -> Mono.just("Ошибка при создании поездки: " + e.getMessage()));
        } catch (Exception e) {
            return Mono.just("Неверный формат даты. Используйте формат YYYY-MM-DD");
        }
    }

    public Mono<String> handleAddPoint(Long chatId, String tripName, String name, double lat, double lon) {
        return service.getAllPlannedTrips(chatId)
                .filter(trip -> trip.getName().equals(tripName))
                .next()
                .flatMap(trip -> service.createPoint(trip.getId(), name, lat, lon)
                        .map(point -> "Точка \"" + point.getName() + "\" добавлена."))
                .switchIfEmpty(Mono.just("Такой поездки нет! Если хотите создать поездку воспользуйтесь: /plantrip или просмотрите свои поездки с помощью: /showplanned"));
    }

    public Mono<String> handleSetStartPoint(String tripId, double latitude, double longitude) {
        return service.setStartPoint(tripId, latitude, longitude)
                .map(trip -> "✅ Стартовая точка успешно добавлена!");
    }

    public Mono<String> handleAddRoute(String tripId, String pointToId, String date) {
        return service.createRoute(tripId, pointToId, LocalDate.parse(date))
                .map(p -> "Маршрут к точке \"" + p.getName() + "\" добавлен.");
    }

    public Mono<String> handleFinishPlanning(Long chatId) {
        return service.finishPlanning(chatId)
                .map(trip -> "Поездка \"" + trip.getName() + "\" перенесена в запланированные.");
    }

    public Mono<String> handleCancelPlanning(Long chatId) {
        return service.cancelPlanning(chatId)
                .map(trip -> "Планирование поездки \"" + trip.getName() + "\" отменено.");
    }

    public Mono<String> handleDeletePlanned(Long chatId, String tripId) {
        return service.deletePlannedTrip(chatId, tripId)
                .map(trip -> "Поездка \"" + trip.getName() + "\" удалена.");
    }
}
