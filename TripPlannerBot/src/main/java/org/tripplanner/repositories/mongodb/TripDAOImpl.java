package org.tripplanner.repositories.mongodb;

import java.time.LocalDate;
import java.util.Collections;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.tripplanner.domain.Point;
import org.tripplanner.domain.Trip;
import org.tripplanner.repositories.PointDAO;
import org.tripplanner.repositories.RouteDAO;
import org.tripplanner.repositories.TripDAO;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class TripDAOImpl implements TripDAO {

    private final ReactiveMongoTemplate mongoTemplate;
    private final TripMapper tripMapper;
    private final PointMapper pointMapper;
    private final RouteMapper routeMapper;
    private final PointDAO pointDAO;
    private final RouteDAO routeDAO;
    private static final Logger logger = LoggerFactory.getLogger(TripDAOImpl.class);
    @Autowired
    public TripDAOImpl(ReactiveMongoTemplate mongoTemplate,
                       TripMapper tripMapper,
                       PointMapper pointMapper, RouteMapper routeMapper,
                       PointDAO pointDAO, RouteDAO routeDAO) {
        this.mongoTemplate = mongoTemplate;
        this.tripMapper = tripMapper;
        this.pointMapper = pointMapper;
        this.routeMapper = routeMapper;
        this.pointDAO = pointDAO;
        this.routeDAO = routeDAO;
    }


    @Override
    public Mono<Trip> createTrip(Long chatId, String name, LocalDate startDate, LocalDate endDate) {
        Trip trip = new Trip(name, startDate, endDate, 0, null, Collections.emptyList(), Collections.emptyList());
        TripDBO dbo = tripMapper.toDbo(trip);
        return mongoTemplate.insert(dbo)
                .doOnNext(savedTrip -> System.out.println("Created trip with ID: " + savedTrip.getId()))
                .flatMap(savedTrip -> {
                    // Update user's plannedTrips list
                    Query userQuery = Query.query(Criteria.where("chatId").is(chatId));
                    Update userUpdate = new Update().addToSet("plannedTrips", savedTrip.getId());
                    return mongoTemplate.findAndModify(userQuery, userUpdate, UserDBO.class)
                            .doOnNext(user -> System.out.println("Updated user's planned trips. New count: " + 
                                    (user.getPlannedTrips() != null ? user.getPlannedTrips().size() : 0)))
                            .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                            .thenReturn(tripMapper.fromDbo(savedTrip));
                });
    }

    @Override
    public Mono<Trip> addPoint(String tripId, String pointId) {
        try {
        ObjectId tripObjectId = new ObjectId(tripId);
        ObjectId pointObjectId = new ObjectId(pointId);

        Query query = Query.query(Criteria.where("_id").is(tripObjectId));
        Update update = new Update().addToSet("points", pointObjectId);

        return mongoTemplate.findAndModify(query, update, TripDBO.class)
                .map(tripMapper::fromDbo);
        } catch (IllegalArgumentException e) {
            return Mono.error(new IllegalArgumentException("Invalid ObjectId format: " + e.getMessage()));
        }
    }


    @Override
    public Mono<Trip> setStartPoint(String tripId, String coordinates) {
        String[] parts = coordinates.split(",");
        double latitude = Double.parseDouble(parts[0].trim());
        double longitude = Double.parseDouble(parts[1].trim());
        
        return mongoTemplate.findById(tripId, Trip.class)
                .flatMap(trip -> {
                    Point point = new Point();
                    point.setLatitude(latitude);
                    point.setLongitude(longitude);
                    trip.setStartPoint(point);
                    return mongoTemplate.save(trip);
                });
    }


    @Override
    public Mono<Trip> addRoute(String tripId, String routeId) {
        ObjectId tripObjectId = new ObjectId(tripId);
        ObjectId routeObjectId = new ObjectId(routeId);

        Query query = Query.query(Criteria.where("_id").is(tripObjectId));
        Update update = new Update().addToSet("routes", routeObjectId);

        return mongoTemplate.findAndModify(query, update, TripDBO.class)
                .map(tripMapper::fromDbo);
    }


    @Override
    public Flux<Point> getAllPoints(String tripId) {
        ObjectId tripObjectId = new ObjectId(tripId);

        return mongoTemplate.findById(tripObjectId, TripDBO.class)
                .flatMapMany(trip -> {
                    if (trip.getPoints() == null || trip.getPoints().isEmpty()) {
                        return Flux.empty();
                    }

                    Query query = Query.query(Criteria.where("_id").in(trip.getPoints()));
                    return mongoTemplate.find(query, PointDBO.class)
                            .map(pointMapper::fromDbo);
                });
    }


    @Override
    public Mono<Trip> setTripRating(String tripId, int rating) {
        ObjectId tripObjectId = new ObjectId(tripId);

        Query query = Query.query(Criteria.where("_id").is(tripObjectId));
        Update update = new Update().set("rating", rating);

        return mongoTemplate.findAndModify(query, update, TripDBO.class)
                .map(tripMapper::fromDbo);
    }


    @Override
    public Mono<Trip> getTrip(String tripId) {
        System.out.println("Looking for trip with ID: " + tripId);
        return mongoTemplate.findOne(
                        Query.query(Criteria.where("_id").is(new ObjectId(tripId))),
                        TripDBO.class
                )
                .doOnNext(trip -> System.out.println("Found trip in DB: \"" + trip.getName() + "\" with ID: " + trip.getId()))
                .switchIfEmpty(Mono.error(new RuntimeException("Trip not found with ID: " + tripId)))
                .map(trip -> {
                    Trip mappedTrip = tripMapper.fromDbo(trip);
                    System.out.println("Successfully mapped trip: " + mappedTrip.getName() + " with ID: " + mappedTrip.getId());
                    return mappedTrip;
                })
                .doOnError(e -> System.out.println("Error in getTrip: " + e.getMessage()));
    }

    @Override
    public Flux<Trip> getAllTrips() {
        return mongoTemplate.findAll(TripDBO.class)
                .map(tripMapper::fromDbo);
    }

    public Mono<Trip> addNoteToTrip(String tripId, String note) {
        try {
            ObjectId id = new ObjectId(tripId);
            Query query = new Query(Criteria.where("_id").is(id));
            Update update = new Update().push("notes", note);
            
            return mongoTemplate.findAndModify(query, update, TripDBO.class)
                    .switchIfEmpty(Mono.error(new RuntimeException("Trip not found with ID: " + tripId)))
                    .map(tripMapper::fromDbo)
                    .onErrorResume(e -> {
                        logger.error("Error adding note to trip {}: {}", tripId, e.getMessage());
                        return Mono.error(e);
                    });
        } catch (IllegalArgumentException e) {
            logger.error("Invalid trip ID format: {}", tripId);
            return Mono.error(new RuntimeException("Invalid trip ID format"));
        }
    }

    @Override
    public Mono<Trip> updateTripStatus(String tripId, String status) {
        try {
            ObjectId id = new ObjectId(tripId);
            Query query = new Query(Criteria.where("_id").is(id));
            
            // Сначала получаем текущую поездку
            return mongoTemplate.findOne(query, TripDBO.class)
                    .switchIfEmpty(Mono.error(new RuntimeException("Trip not found with ID: " + tripId)))
                    .flatMap(tripDBO -> {
                        // Обновляем только статус, сохраняя все остальные данные
                        Update update = new Update()
                                .set("status", status)
                                .set("name", tripDBO.getName())
                                .set("startDate", tripDBO.getStartDate())
                                .set("endDate", tripDBO.getEndDate())
                                .set("rating", tripDBO.getRating())
                                .set("notes", tripDBO.getNotes())
                                .set("points", tripDBO.getPoints())
                                .set("routes", tripDBO.getRoutes())
                                .set("startPoint", tripDBO.getStartPoint());

                        return mongoTemplate.findAndModify(query, update, TripDBO.class)
                                .map(tripMapper::fromDbo);
                    })
                    .onErrorResume(e -> {
                        logger.error("Error updating trip status for trip {}: {}", tripId, e.getMessage());
                        return Mono.error(e);
                    });
        } catch (IllegalArgumentException e) {
            logger.error("Invalid trip ID format: {}", tripId);
            return Mono.error(new RuntimeException("Invalid trip ID format"));
        }
    }

    @Override
    public Mono<Point> markPointVisited(String tripId, String pointId) {
        try {
            ObjectId tripObjectId = new ObjectId(tripId);
            ObjectId pointObjectId = new ObjectId(pointId);

            // First get the point to mark it as visited
            return mongoTemplate.findById(pointObjectId, PointDBO.class)
                    .switchIfEmpty(Mono.error(new RuntimeException("Point not found with ID: " + pointId)))
                    .flatMap(pointDBO -> {
                        // Update the point's visited status
                        Query pointQuery = Query.query(Criteria.where("_id").is(pointObjectId));
                        Update pointUpdate = new Update().set("visited", true);
                        
                        return mongoTemplate.findAndModify(pointQuery, pointUpdate, PointDBO.class)
                                .map(pointMapper::fromDbo);
                    });
        } catch (IllegalArgumentException e) {
            logger.error("Invalid ObjectId format: {}", e.getMessage());
            return Mono.error(new RuntimeException("Invalid ObjectId format"));
        }
    }

    // Остальные методы будут добавлены позже
}
