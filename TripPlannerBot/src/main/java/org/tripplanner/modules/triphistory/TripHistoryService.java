package org.tripplanner.modules.triphistory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tripplanner.domain.Point;
import org.tripplanner.domain.Trip;
import org.tripplanner.repositories.TripDAO;
import org.tripplanner.repositories.UserDAO;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TripHistoryService {

    private final UserDAO userDAO;
    private final TripDAO tripDAO;
    private static final Logger logger = LoggerFactory.getLogger(TripHistoryService.class);

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
        logger.info("Setting rating {} for trip {}", rating, tripId);
        return tripDAO.getTrip(tripId)
                .flatMap(trip -> {
                    if (trip == null) {
                        return Mono.error(new RuntimeException("Поездка не найдена"));
                    }
                    return tripDAO.setTripRating(tripId, rating)
                            .doOnNext(t -> logger.info("Rating {} set for trip {}", rating, tripId));
                });
    }

    public Flux<Trip> getAllFinishedTrips(Long chatId) {
        return userDAO.getUserByChatId(chatId)
                .flatMapMany(user -> {
                    logger.info("Getting finished trips for user {}: {}", chatId, user.getTripHistory());
                    return Flux.fromIterable(user.getTripHistory())
                            .flatMap(tripId -> tripDAO.getTrip(tripId.getId())
                                    .doOnNext(trip -> logger.info("Found finished trip: {} with ID: {}", trip.getName(), trip.getId()))
                                    .onErrorResume(e -> {
                                        logger.error("Error getting trip {}: {}", tripId, e.getMessage());
                                        return Mono.empty();
                                    }));
                });
    }

    public Mono<Trip> findFinishedTripByName(Long chatId, String tripName) {
        return getAllFinishedTrips(chatId)
                .filter(trip -> trip.getName().equals(tripName))
                .next()
                .doOnNext(trip -> logger.info("Found finished trip by name {}: {}", tripName, trip.getId()))
                .doOnError(e -> logger.error("Error finding finished trip by name {}: {}", tripName, e.getMessage()));
    }

    public Flux<Point> getAllPoints(String tripId) {
        return tripDAO.getAllPoints(tripId);
    }
}
