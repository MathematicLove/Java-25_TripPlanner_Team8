package org.tripplanner.repositories;

import org.tripplanner.domain.Trip;
import org.tripplanner.domain.User;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserDAO {
    Mono<User> getOrCreateUser(Long chatId);

    Flux<User> getAllUsers();

    Flux<Trip> getAllPlannedTrips(Long chatId);

    Mono<Trip> getTripInPlanning(Long chatId);

    Mono<Trip> finishPlanning(Long chatId);

    Mono<Trip> cancelPlanning(Long chatId);

    Mono<Trip> deletePlannedTrip(Long chatId, String tripId);

    Flux<Trip> getCurrentTrips(Long chatId);

    Flux<Trip> getFinishedTrips(Long chatId);

    Mono<Void> updateUserLocation(Long chatId, double latitude, double longitude);
}
