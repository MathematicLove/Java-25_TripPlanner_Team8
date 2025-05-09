package org.tripplanner.repositories.mongodb;

import java.util.Collections;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.tripplanner.domain.Point;
import org.tripplanner.repositories.PointDAO;

import reactor.core.publisher.Mono;

@Repository
public class PointDAOImpl implements PointDAO {

    private final ReactiveMongoTemplate mongoTemplate;
    private final PointMapper pointMapper;

    @Autowired
    public PointDAOImpl(ReactiveMongoTemplate mongoTemplate, PointMapper pointMapper) {
        this.mongoTemplate = mongoTemplate;
        this.pointMapper = pointMapper;
    }

    @Override
    public Mono<Point> createPoint(Long chatId, Long tripId, String name, double latitude, double longitude) {
        Point point = new Point(name, latitude, longitude, false, Collections.emptyList());
        PointDBO dbo = pointMapper.toDbo(point);
        return mongoTemplate.insert(dbo)
                .map(pointMapper::fromDbo);
    }

    @Override
    public Mono<Point> markPointVisited(String pointId) {
        Query query = Query.query(Criteria.where("_id").is(new ObjectId(pointId)));
        Update update = new Update().set("visited", true);

        return mongoTemplate.findAndModify(query, update, PointDBO.class)
                .map(pointMapper::fromDbo);
    }

    @Override
    public Mono<Point> addNoteToPoint(String pointId, String note) {
        Query query = Query.query(Criteria.where("_id").is(new ObjectId(pointId)));
        Update update = new Update().push("notes", note);

        return mongoTemplate.findAndModify(query, update, PointDBO.class)
                .map(pointMapper::fromDbo);
    }

    @Override
    public Mono<Point> getPoint(String pointId) {
        return mongoTemplate.findOne(
                Query.query(Criteria.where("_id").is(new ObjectId(pointId))),
                PointDBO.class
        ).map(pointMapper::fromDbo);
    }
}
