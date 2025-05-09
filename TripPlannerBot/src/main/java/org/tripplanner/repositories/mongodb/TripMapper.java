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
        dbo.setId(new ObjectId());
        dbo.setName(trip.getName());
        dbo.setStartDate(trip.getStartDate());
        dbo.setEndDate(trip.getEndDate());
        dbo.setRating(trip.getRating());

        // Если startPoint не null, сохраняем его ID
        dbo.setStartPoint(trip.getStartPoint() != null ? new ObjectId() : null);

        // Преобразование points → List<ObjectId>
        dbo.setPoints(trip.getPoints() != null ?
                trip.getPoints().stream().map(p -> new ObjectId()).collect(Collectors.toList()) :
                Collections.emptyList());

        // Преобразование routes → List<ObjectId>
        dbo.setRoutes(trip.getRoutes() != null ?
                trip.getRoutes().stream().map(r -> new ObjectId()).collect(Collectors.toList()) :
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
        trip.setStartPoint(null);
        trip.setPoints(Collections.emptyList());
        trip.setRoutes(Collections.emptyList());

        System.out.println("Mapped trip from DBO: " + trip.getName() + " with ID: " + trip.getId() + 
                ", dates: " + trip.getStartDate() + " - " + trip.getEndDate() + 
                ", rating: " + trip.getRating());
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
