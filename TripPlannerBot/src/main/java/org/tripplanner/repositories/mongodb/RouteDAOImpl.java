package org.tripplanner.repositories.mongodb;

import java.time.LocalDate;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.tripplanner.domain.Route;
import org.tripplanner.repositories.RouteDAO;

import reactor.core.publisher.Mono;

@Repository
public class RouteDAOImpl implements RouteDAO {

    private final ReactiveMongoTemplate mongoTemplate;
    private final RouteMapper routeMapper;

    @Autowired
    public RouteDAOImpl(ReactiveMongoTemplate mongoTemplate, RouteMapper routeMapper) {
        this.mongoTemplate = mongoTemplate;
        this.routeMapper = routeMapper;
    }

    @Override
    public Mono<Route> createRoute(String pointToId, String startDate, String endDate) {
        ObjectId pointTo = new ObjectId(pointToId);
        Route route = new Route(LocalDate.parse(startDate), LocalDate.parse(endDate), null); // pointTo будет добавлен маппером
        RouteDBO dbo = routeMapper.toDbo(route, pointTo);

        return mongoTemplate.insert(dbo)
                .map(routeMapper::fromDbo);
    }

    @Override
    public Mono<Route> findById(String routeId) {
        return mongoTemplate.findById(new ObjectId(routeId), RouteDBO.class)
                .map(routeMapper::fromDbo);
    }

    @Override
    public Mono<Route> getRoute(String routeId) {
        return mongoTemplate.findOne(
                Query.query(Criteria.where("_id").is(new ObjectId(routeId))),
                RouteDBO.class
        ).map(routeMapper::fromDbo);
    }
}
