package org.tripplanner.repositories;

import org.tripplanner.domain.Point;

import reactor.core.publisher.Mono;

public interface PointDAO {

    Mono<Point> createPoint(Long chatId, Long tripId, String name, double latitude, double longitude);

    Mono<Point> markPointVisited(String pointId);

    Mono<Point> addNoteToPoint(String pointId, String note);

    Mono<Point> getPoint(String pointId);
}
