package org.tripplanner.repositories.mongodb;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tripplanner.domain.Point;

@Component
public class PointMapper {
    private static final Logger logger = LoggerFactory.getLogger(PointMapper.class);

    public Point fromDbo(PointDBO dbo) {
        if (dbo == null) {
            logger.error("Cannot map null PointDBO");
            return null;
        }

        logger.info("Mapping point from DBO: id={}, name={}, visited={}", 
            dbo.getId(), dbo.getName(), dbo.isVisited());

        Point point = new Point(
                dbo.getName(),
                dbo.getLatitude(),
                dbo.getLongitude(),
                dbo.isVisited(),
                dbo.getNotes()
        );
        point.setId(dbo.getId().toHexString());
        
        logger.info("Mapped point: id={}, name={}, visited={}", 
            point.getId(), point.getName(), point.isVisited());
        return point;
    }

    public PointDBO toDbo(Point point) {
        if (point == null) {
            logger.error("Cannot map null Point");
            return null;
        }

        logger.info("Mapping point to DBO: id={}, name={}, visited={}", 
            point.getId(), point.getName(), point.isVisited());

        PointDBO dbo = new PointDBO();
        if (point.getId() != null) {
            dbo.setId(new ObjectId(point.getId()));
        } else {
            dbo.setId(new ObjectId());
        }
        dbo.setName(point.getName());
        dbo.setLatitude(point.getLatitude());
        dbo.setLongitude(point.getLongitude());
        dbo.setVisited(point.isVisited());
        dbo.setNotes(point.getNotes());

        logger.info("Mapped to DBO: id={}, name={}, visited={}", 
            dbo.getId(), dbo.getName(), dbo.isVisited());
        return dbo;
    }
}
