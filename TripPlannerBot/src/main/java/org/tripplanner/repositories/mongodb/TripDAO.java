package org.tripplanner.repositories.mongodb;

import java.time.LocalDate;

import org.tripplanner.domain.Point;
import org.tripplanner.domain.Trip;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TripDAO {
    Mono<Trip> createTrip(Long chatId, String name, LocalDate startDate, LocalDate endDate);
    Mono<Trip> setStartPoint(String tripId, String coordinates);
    Mono<Trip> addPoint(String tripId, String pointId);
    Mono<Trip> addRoute(String tripId, String routeId);
    Mono<Trip> finishPlanning(Long chatId);
    Mono<Trip> cancelPlanning(Long chatId);
    Mono<Trip> deletePlannedTrip(Long chatId, String tripId);
    Flux<Trip> getAllPlannedTrips(Long chatId);
    Flux<Point> getAllPoints(String tripId);
    Mono<Trip> setTripRating(String tripId, int rating);
    Mono<Trip> getTrip(String tripId);
    Flux<Trip> getAllTrips();
} 