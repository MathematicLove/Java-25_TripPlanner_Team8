package org.tripplanner.repositories;

import java.time.LocalDate;

import org.tripplanner.domain.Point;
import org.tripplanner.domain.Trip;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TripDAO {

    Mono<Trip> createTrip(Long chatId, String name, LocalDate startDate, LocalDate endDate);

    Mono<Trip> addPoint(String tripId, String pointId);

    Mono<Trip> setStartPoint(String tripId, String pointId);

    Mono<Trip> addRoute(String tripId, String routeId);

    Flux<Point> getAllPoints(String tripId);

    Mono<Trip> setTripRating(String tripId, int rating);

    Mono<Trip> getTrip(String tripId);

    Flux<Trip> getAllTrips();

    Mono<Trip> addNoteToTrip(String tripId, String note);

    Mono<Trip> updateTripStatus(String tripId, String status);

    Mono<Point> markPointVisited(String tripId, String pointId);
}
