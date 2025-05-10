package org.tripplanner.modules.plannedtrips;

import java.time.LocalDate;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.tripplanner.domain.Point;
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

    public Mono<Trip> finishPlanning(Long chatId) {
        return userDAO.finishPlanning(chatId);
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

    public Mono<Trip> setStartPoint(String tripId, double latitude, double longitude) {
        String coordinates = latitude + "," + longitude;
        return tripDAO.setStartPoint(tripId, coordinates);
    }

    private String extractIdFromRoute(Object route) {
        return new ObjectId().toHexString();
    }
}
