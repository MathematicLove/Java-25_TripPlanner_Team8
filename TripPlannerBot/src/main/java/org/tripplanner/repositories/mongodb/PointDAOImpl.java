package org.tripplanner.repositories.mongodb;

import java.util.Collections;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.tripplanner.domain.Point;
import org.tripplanner.repositories.PointDAO;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class PointDAOImpl implements PointDAO {
    private static final Logger logger = LoggerFactory.getLogger(PointDAOImpl.class);

    private final ReactiveMongoTemplate mongoTemplate;
    private final PointMapper pointMapper;

    @Autowired
    public PointDAOImpl(ReactiveMongoTemplate mongoTemplate, PointMapper pointMapper) {
        this.mongoTemplate = mongoTemplate;
        this.pointMapper = pointMapper;
    }

    @Override
    public Mono<Point> createPoint(Long chatId, String tripId, String name, double latitude, double longitude) {
        logger.info("Creating point: name={}, tripId={}", name, tripId);
        Point point = new Point(name, latitude, longitude, false, Collections.emptyList());
        PointDBO dbo = pointMapper.toDbo(point);
        dbo.setTripId(new ObjectId(tripId));
        return mongoTemplate.insert(dbo)
                .map(pointMapper::fromDbo)
                .doOnNext(p -> logger.info("Created point: id={}, name={}", p.getId(), p.getName()));
    }

    @Override
    public Mono<Point> markPointVisited(String pointId) {
        logger.info("Marking point as visited: id={}", pointId);
        try {
            ObjectId id = new ObjectId(pointId);
            Query query = new Query(Criteria.where("_id").is(id));
            Update update = new Update().set("visited", true);
            return mongoTemplate.findAndModify(query, update, PointDBO.class)
                    .flatMap(pointDBO -> {
                        if (pointDBO == null) {
                            logger.error("Point not found: id={}", pointId);
                            return Mono.error(new RuntimeException("Точка не найдена"));
                        }
                        logger.info("Found point to mark as visited: id={}, name={}", 
                            pointDBO.getId(), pointDBO.getName());
                        return Mono.just(pointMapper.fromDbo(pointDBO));
                    });
        } catch (IllegalArgumentException e) {
            logger.error("Invalid point ID format: {}", pointId);
            return Mono.error(new RuntimeException("Неверный формат ID точки"));
        }
    }

    @Override
    public Mono<Point> addNoteToPoint(String pointId, String note) {
        logger.info("Adding note to point: id={}, note={}", pointId, note);
        Query query = Query.query(Criteria.where("_id").is(new ObjectId(pointId)));
        Update update = new Update().push("notes", note);

        return mongoTemplate.findAndModify(query, update, PointDBO.class)
                .map(pointMapper::fromDbo)
                .doOnNext(p -> logger.info("Added note to point: id={}, name={}", p.getId(), p.getName()));
    }

    @Override
    public Mono<Point> getPoint(String pointId) {
        logger.info("Getting point: id={}", pointId);
        return mongoTemplate.findOne(
                Query.query(Criteria.where("_id").is(new ObjectId(pointId))),
                PointDBO.class
        ).map(pointMapper::fromDbo)
         .doOnNext(p -> logger.info("Retrieved point: id={}, name={}", p.getId(), p.getName()));
    }

    @Override
    public Flux<Point> getPointsByTripId(String tripId) {
        logger.info("Getting points for trip: id={}", tripId);
        try {
            ObjectId tripObjectId = new ObjectId(tripId);
            Query query = new Query(Criteria.where("tripId").is(tripObjectId));
            return mongoTemplate.find(query, PointDBO.class)
                    .doOnNext(dbo -> logger.info("Found point in DB: id={}, name={}", 
                        dbo.getId(), dbo.getName()))
                    .map(pointMapper::fromDbo)
                    .doOnNext(p -> logger.info("Mapped point: id={}, name={}", p.getId(), p.getName()));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid trip ID format: {}", tripId);
            return Flux.error(new RuntimeException("Неверный формат ID поездки"));
        }
    }
}
