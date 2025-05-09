package org.tripplanner.modules.triphelper;

import org.springframework.stereotype.Service;
import org.tripplanner.domain.Point;
import org.tripplanner.domain.Trip;
import org.tripplanner.repositories.PointDAO;
import org.tripplanner.repositories.TripDAO;
import org.tripplanner.repositories.UserDAO;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TripHelperService {

    private final UserDAO userDAO;
    private final TripDAO tripDAO;
    private final PointDAO pointDAO;

    public TripHelperService(UserDAO userDAO, TripDAO tripDAO, PointDAO pointDAO) {
        this.userDAO = userDAO;
        this.tripDAO = tripDAO;
        this.pointDAO = pointDAO;
    }

    public Flux<Trip> getOngoingTrips(Long chatId) {
        return userDAO.getOngoingTrip(chatId).flux(); // оборачиваем Mono → Flux
    }

    public Flux<Point> listAllTripPoints(String tripId) {
        return tripDAO.getAllPoints(tripId);
    }

    public Mono<Point> markPointVisited(String pointId) {
        return pointDAO.markPointVisited(pointId);
    }

    public Mono<Point> addNoteToPoint(String pointId, String note) {
        return pointDAO.addNoteToPoint(pointId, note);
    }
}
