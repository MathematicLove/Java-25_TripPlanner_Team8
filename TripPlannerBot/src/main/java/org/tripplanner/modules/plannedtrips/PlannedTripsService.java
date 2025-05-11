package org.tripplanner.modules.plannedtrips;

import java.time.LocalDate;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tripplanner.domain.Point;
import org.tripplanner.domain.Route;
import org.tripplanner.domain.Trip;
import org.tripplanner.domain.User;
import org.tripplanner.repositories.PointDAO;
import org.tripplanner.repositories.RouteDAO;
import org.tripplanner.repositories.TripDAO;
import org.tripplanner.repositories.UserDAO;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PlannedTripsService {

    private final UserDAO userDAO;
    private final TripDAO tripDAO;
    private final PointDAO pointDAO;
    private final RouteDAO routeDAO;
    private static final Logger logger = LoggerFactory.getLogger(PlannedTripsService.class);

    public PlannedTripsService(UserDAO userDAO, TripDAO tripDAO,
                               PointDAO pointDAO, RouteDAO routeDAO) {
        this.userDAO = userDAO;
        this.tripDAO = tripDAO;
        this.pointDAO = pointDAO;
        this.routeDAO = routeDAO;
    }

    public Mono<User> registerUserIfNeeded(Long chatId) {
        return userDAO.getOrCreateUser(chatId);
    }

    public Flux<Trip> getAllPlannedTrips(Long chatId) {
        return userDAO.getAllPlannedTrips(chatId);
    }

    public Mono<Trip> planTrip(Long chatId, String name, LocalDate startDate, LocalDate endDate) {
        return tripDAO.createTrip(chatId, name, startDate, endDate);
    }

    public Mono<Point> createPoint(String tripId, String name, double latitude, double longitude) {
        return pointDAO.createPoint(0L, tripId, name, latitude, longitude)
                .flatMap(point -> {
                    if (point.getId() == null) {
                        return Mono.error(new IllegalStateException("Point ID is null after creation"));
                    }
                    return tripDAO.addPoint(tripId, point.getId()).thenReturn(point);
                });
    }

    public Mono<Point> createRoute(String tripId, String pointToId, LocalDate date) {
        return routeDAO.createRoute(pointToId, date.toString(), date.toString())
                .flatMap(route -> tripDAO.addRoute(tripId, extractIdFromRoute(route)).thenReturn(route.getPointTo()));
    }

    public Mono<Trip> finishPlanning(Long chatId, String tripName) {
        return userDAO.getUserByChatId(chatId)
                .flatMap(user -> {
                    if (user.getPlannedTrips() == null || user.getPlannedTrips().isEmpty()) {
                        return Mono.error(new RuntimeException("У вас нет запланированных поездок. Сначала создайте поездку с помощью /plantrip"));
                    }
                    // Получаем все запланированные поездки
                    return Flux.fromIterable(user.getPlannedTrips())
                            .flatMap(trip -> tripDAO.getTrip(trip.getId()))
                            .collectList()
                            .flatMap(trips -> {
                                if (trips.isEmpty()) {
                                    return Mono.error(new RuntimeException("Не удалось загрузить запланированные поездки. Попробуйте позже"));
                                }
                                // Ищем поездку по названию
                                return Flux.fromIterable(trips)
                                        .filter(trip -> trip.getName().equals(tripName))
                                        .next()
                                        .switchIfEmpty(Mono.error(new RuntimeException("Поездка с названием '" + tripName + "' не найдена. Проверьте название и попробуйте снова")))
                                        .flatMap(trip -> {
                                            String tripId = trip.getId();
                                            // Добавляем поездку в историю и удаляем из запланированных
                                            return userDAO.addToTripHistory(chatId, tripId)
                                                    .then(userDAO.removeFromPlannedTrips(chatId, tripId))
                                                    .thenReturn(trip);
                                        });
                            });
                })
                .onErrorResume(e -> {
                    logger.error("Error in finishPlanning for user {} and trip {}: {}", chatId, tripName, e.getMessage());
                    return Mono.error(e);
                });
    }

    // Старый метод для обратной совместимости
    public Mono<Trip> finishPlanning(Long chatId) {
        return userDAO.getUserByChatId(chatId)
                .flatMap(user -> {
                    if (user.getPlannedTrips().isEmpty()) {
                        return Mono.error(new RuntimeException("У вас нет запланированных поездок"));
                    }
                    // Получаем все запланированные поездки
                    return Flux.fromIterable(user.getPlannedTrips())
                            .flatMap(trip -> tripDAO.getTrip(trip.getId()))
                            .collectList()
                            .flatMap(trips -> {
                                if (trips.isEmpty()) {
                                    return Mono.error(new RuntimeException("Не удалось загрузить запланированные поездки"));
                                }
                                // Берем первую поездку
                                Trip trip = trips.get(0);
                                String tripId = trip.getId();
                                // Добавляем поездку в историю
                                return userDAO.addToTripHistory(chatId, tripId)
                                        // Удаляем поездку из запланированных
                                        .then(userDAO.removeFromPlannedTrips(chatId, tripId))
                                        .thenReturn(trip);
                            });
                });
    }

    public Mono<Trip> cancelPlanning(Long chatId) {
        return userDAO.cancelPlanning(chatId);
    }

    public Mono<Trip> deletePlannedTrip(Long chatId, String tripId) {
        return userDAO.deletePlannedTrip(chatId, tripId);
    }

    private String extractIdFromPoint(Point point) {
        return new ObjectId().toHexString();
    }

    private String extractIdFromRoute(Object route) {
        return new ObjectId().toHexString();
    }

    public Mono<Route> addRoute(String tripId, String pointName, String routeDate) {
        return tripDAO.getTrip(tripId)
                .flatMap(trip -> pointDAO.getPointsByTripId(tripId)
                        .filter(point -> point.getName().equals(pointName))
                        .next()
                        .switchIfEmpty(Mono.error(new RuntimeException("Точка с таким названием не найдена в поездке.")))
                        .flatMap(point -> {
                            logger.debug("Found point: {}", point);
                            return routeDAO.createRoute(point.getId(), routeDate, routeDate)
                                    .flatMap(route -> {
                                        if (route.getPointTo() == null) {
                                            return Mono.error(new RuntimeException("Не удалось создать маршрут: точка назначения не найдена"));
                                        }
                                        return tripDAO.addRoute(tripId, route.getPointTo().getId())
                                                .thenReturn(route);
                                    });
                        })
                );
    }
}
