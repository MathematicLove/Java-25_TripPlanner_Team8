package org.tripplanner.repositories.mongodb;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;
import org.tripplanner.domain.Route;

@Component
public class RouteMapper {

    public Route fromDbo(RouteDBO dbo) {
        if (dbo == null) return null;

        return new Route(
                dbo.getStartDate(),
                dbo.getEndDate(),
                null // Point pointTo загружается отдельно (по ObjectId)
        );
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
