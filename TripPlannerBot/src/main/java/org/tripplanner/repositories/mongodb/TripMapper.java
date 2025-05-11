package org.tripplanner.repositories.mongodb;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;
import org.tripplanner.domain.Point;
import org.tripplanner.domain.Route;
import org.tripplanner.domain.Trip;

@Component
public class TripMapper {

    private final PointMapper pointMapper;
    private final RouteMapper routeMapper;

    public TripMapper(PointMapper pointMapper, RouteMapper routeMapper) {
        this.pointMapper = pointMapper;
        this.routeMapper = routeMapper;
    }

    public TripDBO toDbo(Trip trip) {
        TripDBO dbo = new TripDBO();
        if (trip.getId() != null && !trip.getId().isEmpty()) {
            dbo.setId(new ObjectId(trip.getId()));
        }
        dbo.setName(trip.getName());
        dbo.setStartDate(trip.getStartDate());
        dbo.setEndDate(trip.getEndDate());
        dbo.setRating(trip.getRating());
        dbo.setStatus(trip.getStatus());
        dbo.setNotes(trip.getNotes() != null ? trip.getNotes() : Collections.emptyList());

        // Если startPoint не null, сохраняем его ID
        if (trip.getStartPoint() != null && trip.getStartPoint().getId() != null) {
            dbo.setStartPoint(new ObjectId(trip.getStartPoint().getId()));
        }

        // Преобразование points → List<ObjectId>
        dbo.setPoints(trip.getPoints() != null ?
                trip.getPoints().stream()
                    .filter(p -> p.getId() != null)
                    .map(p -> new ObjectId(p.getId()))
                    .collect(Collectors.toList()) :
                Collections.emptyList());

        // Преобразование routes → List<ObjectId>
        dbo.setRoutes(trip.getRoutes() != null ?
                trip.getRoutes().stream()
                    .filter(r -> r != null)
                    .map(r -> new ObjectId())
                    .collect(Collectors.toList()) :
                Collections.emptyList());

        return dbo;
    }

    public Trip fromDbo(TripDBO dbo) {
        if (dbo == null) {
            System.out.println("Cannot map null TripDBO");
            return null;
        }

        Trip trip = new Trip();
        trip.setId(dbo.getId().toHexString());
        trip.setName(dbo.getName());
        trip.setStartDate(dbo.getStartDate());
        trip.setEndDate(dbo.getEndDate());
        trip.setRating(dbo.getRating());
        trip.setStatus(dbo.getStatus());
        trip.setNotes(dbo.getNotes() != null ? dbo.getNotes() : Collections.emptyList());

        // Маппинг startPoint
        if (dbo.getStartPoint() != null) {
            Point point = new Point();
            point.setId(dbo.getStartPoint().toHexString());
            trip.setStartPoint(point);
        }

        // Маппинг points
        trip.setPoints(dbo.getPoints() != null ? 
                dbo.getPoints().stream()
                        .map(pointId -> {
                            Point point = new Point();
                            point.setId(pointId.toHexString());
                            return point;
                        })
                        .collect(Collectors.toList()) : 
                Collections.emptyList());

        // Маппинг routes
        trip.setRoutes(dbo.getRoutes() != null ?
                dbo.getRoutes().stream()
                        .map(routeId -> {
                            Route route = new Route();
                            return route;
                        })
                        .collect(Collectors.toList()) :
                Collections.emptyList());

        System.out.println("Mapped trip from DBO: " + trip.getName() + " with ID: " + trip.getId() + 
                ", dates: " + trip.getStartDate() + " - " + trip.getEndDate() + 
                ", rating: " + trip.getRating() +
                ", points: " + trip.getPoints().size() +
                ", routes: " + trip.getRoutes().size() +
                ", status: " + trip.getStatus() +
                ", notes: " + trip.getNotes().size());
        return trip;
    }

    public Trip fromDbo(TripDBO dbo, Point startPoint, List<Point> points, List<Route> routes) {
        Trip trip = fromDbo(dbo);
        if (trip != null) {
            trip.setStartPoint(startPoint);
            trip.setPoints(points != null ? points : Collections.emptyList());
            trip.setRoutes(routes != null ? routes : Collections.emptyList());
            System.out.println("Mapped trip with related data: " + trip.getName() + " with ID: " + trip.getId() + 
                    ", points: " + trip.getPoints().size() + 
                    ", routes: " + trip.getRoutes().size() + 
                    ", has start point: " + (trip.getStartPoint() != null) +
                    ", dates: " + trip.getStartDate() + " - " + trip.getEndDate() +
                    ", rating: " + trip.getRating());
        }
        return trip;
    }
}
