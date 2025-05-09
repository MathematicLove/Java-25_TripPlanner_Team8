package org.tripplanner.repositories.mongodb;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;
import org.tripplanner.domain.Point;

@Component
public class PointMapper {

    public Point fromDbo(PointDBO dbo) {
        if (dbo == null) return null;

        return new Point(
                dbo.getName(),
                dbo.getLatitude(),
                dbo.getLongitude(),
                dbo.isVisited(),
                dbo.getNotes()
        );
    }

    public PointDBO toDbo(Point point) {
        if (point == null) return null;

        PointDBO dbo = new PointDBO();
        dbo.setId(new ObjectId()); // создаём новый ObjectId
        dbo.setName(point.getName());
        dbo.setLatitude(point.getLatitude());
        dbo.setLongitude(point.getLongitude());
        dbo.setVisited(point.isVisited());
        dbo.setNotes(point.getNotes());
        return dbo;
    }
}
