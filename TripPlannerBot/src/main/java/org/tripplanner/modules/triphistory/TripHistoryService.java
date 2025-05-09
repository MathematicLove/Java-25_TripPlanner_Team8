package org.tripplanner.modules.triphistory;

import org.springframework.stereotype.Service;
import org.tripplanner.domain.Trip;
import org.tripplanner.repositories.TripDAO;
import org.tripplanner.repositories.UserDAO;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TripHistoryService {

    private final UserDAO userDAO;
    private final TripDAO tripDAO;

    public TripHistoryService(UserDAO userDAO, TripDAO tripDAO) {
        this.userDAO = userDAO;
        this.tripDAO = tripDAO;
    }

    public Flux<Trip> getFinishedTrips(Long userId) {
        return userDAO.getFinishedTrips(userId);
    }

    public Mono<Trip> getFinishedTrip(String tripId) {
        return tripDAO.getTrip(tripId);
    }

    public Mono<Trip> setFinishedTripRating(String tripId, int rating) {
        return tripDAO.setTripRating(tripId, rating);
    }
}
