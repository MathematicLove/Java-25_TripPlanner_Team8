package org.tripplanner.repositories;

import org.tripplanner.domain.Route;

import reactor.core.publisher.Mono;

public interface RouteDAO {

    Mono<Route> createRoute(String pointToId, String startDate, String endDate);

    Mono<Route> findById(String routeId);

    Mono<Route> getRoute(String routeId);
}
