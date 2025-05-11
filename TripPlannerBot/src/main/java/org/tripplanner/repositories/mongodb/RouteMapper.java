package org.tripplanner.repositories.mongodb;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tripplanner.domain.Route;
import org.tripplanner.repositories.PointDAO;

@Component
public class RouteMapper {

    private final PointDAO pointDAO;

    @Autowired
    public RouteMapper(PointDAO pointDAO) {
        this.pointDAO = pointDAO;
    }

    public Route fromDbo(RouteDBO dbo) {
        if (dbo == null) return null;

        Route route = new Route(
                dbo.getStartDate(),
                dbo.getEndDate(),
                null
        );

        if (dbo.getPointTo() != null) {
            return pointDAO.getPoint(dbo.getPointTo().toHexString())
                    .map(point -> {
                        route.setPointTo(point);
                        return route;
                    })
                    .block();
        }

        return route;
    }

    public RouteDBO toDbo(Route route, ObjectId pointToId) {
        if (route == null) return null;

        RouteDBO dbo = new RouteDBO();
        dbo.setId(new ObjectId());
        dbo.setStartDate(route.getStartDate());
        dbo.setEndDate(route.getEndDate());
        dbo.setPointTo(pointToId);
        return dbo;
    }
}
