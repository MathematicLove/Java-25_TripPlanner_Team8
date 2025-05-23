package org.tripplanner.repositories;

import org.tripplanner.domain.Point;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PointDAO {

    Mono<Point> createPoint(Long chatId, String tripId, String name, double latitude, double longitude);

    Mono<Point> markPointVisited(String pointId);

    Mono<Point> addNoteToPoint(String pointId, String note);

    Mono<Point> getPoint(String pointId);

    Flux<Point> getPointsByTripId(String tripId);
}
