package org.tripplanner.repositories.mongodb;

import java.util.ArrayList;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.tripplanner.domain.Trip;
import org.tripplanner.domain.User;
import org.tripplanner.repositories.TripDAO;
import org.tripplanner.repositories.UserDAO;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class UserDAOImpl implements UserDAO {

    private static final Logger logger = LoggerFactory.getLogger(UserDAOImpl.class);
    private final ReactiveMongoTemplate mongoTemplate;
    private final UserMapper userMapper;
    private final TripDAO tripDAO;

    @Autowired
    public UserDAOImpl(ReactiveMongoTemplate mongoTemplate, UserMapper userMapper, TripDAO tripDAO) {
        this.mongoTemplate = mongoTemplate;
        this.userMapper = userMapper;
        this.tripDAO = tripDAO;
        
        // Создаем уникальный индекс для chatId
        mongoTemplate.indexOps(UserDBO.class)
                .ensureIndex(new org.springframework.data.mongodb.core.index.Index()
                        .on("chatId", org.springframework.data.domain.Sort.Direction.ASC)
                        .unique())
                .onErrorResume(e -> {
                    logger.error("Failed to create index for chatId: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<User> getOrCreateUser(Long chatId) {
        Query query = Query.query(Criteria.where("chatId").is(chatId));
        return mongoTemplate.findOne(query, UserDBO.class)
                .flatMap(dbo -> Mono.just(userMapper.fromDbo(dbo)))
                .switchIfEmpty(
                        mongoTemplate.insert(newUserDbo(chatId))
                                .map(userMapper::fromDbo)
                )
                .onErrorResume(e -> {
                    logger.error("Error in getOrCreateUser for chatId {}: {}", chatId, e.getMessage());
                    if (e.getMessage().contains("duplicate key error")) {
                        return mongoTemplate.findOne(query, UserDBO.class)
                                .map(userMapper::fromDbo);
                    }
                    return Mono.error(e);
                });
    }

    @Override
    public Flux<User> getAllUsers() {
        return mongoTemplate.findAll(UserDBO.class)
                .map(userMapper::fromDbo)
                .onErrorResume(e -> {
                    logger.error("Error in getAllUsers: {}", e.getMessage());
                    return Flux.error(e);
                });
    }

    @Override
    public Flux<Trip> getAllPlannedTrips(Long chatId) {
        return mongoTemplate.findOne(
                Query.query(Criteria.where("chatId").is(chatId)),
                UserDBO.class
        )
                .flatMapMany(user -> {
                    if (user.getPlannedTrips() == null || user.getPlannedTrips().isEmpty()) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(user.getPlannedTrips())
                            .flatMap(tripId -> tripDAO.getTrip(tripId.toHexString())
                                    .onErrorResume(e -> {
                                        logger.error("Error getting trip {} for user {}: {}", 
                                            tripId, chatId, e.getMessage());
                                        return Mono.empty();
                                    }));
                })
                .onErrorResume(e -> {
                    logger.error("Error in getAllPlannedTrips for chatId {}: {}", chatId, e.getMessage());
                    return Flux.error(e);
                });
    }

    @Override
    public Mono<Trip> getTripInPlanning(Long chatId) {
        return mongoTemplate.findOne(
                Query.query(Criteria.where("chatId").is(chatId)),
                UserDBO.class
        )
                .flatMap(user -> {
                    if (user.getTripInPlanning() == null) return Mono.empty();
                    return tripDAO.getTrip(user.getTripInPlanning().toHexString())
                            .onErrorResume(e -> {
                                logger.error("Error getting trip in planning for user {}: {}", 
                                    chatId, e.getMessage());
                                return Mono.empty();
                            });
                })
                .onErrorResume(e -> {
                    logger.error("Error in getTripInPlanning for chatId {}: {}", chatId, e.getMessage());
                    return Mono.error(e);
                });
    }

    @Override
    public Mono<Trip> finishPlanning(Long chatId) {
        Query query = Query.query(Criteria.where("chatId").is(chatId));
        return mongoTemplate.findOne(query, UserDBO.class)
                .flatMap(user -> {
                    ObjectId tripId = user.getTripInPlanning();
                    if (tripId == null) {
                        return Mono.error(new RuntimeException("No trip in planning"));
                    }

                    // Сначала получаем поездку, чтобы сохранить все данные
                    return tripDAO.getTrip(tripId.toString())
                            .flatMap(trip -> {
                                // Обновляем статус поездки
                                return tripDAO.updateTripStatus(tripId.toString(), "FINISHED")
                                        .then(Mono.just(trip));
                            })
                            .flatMap(trip -> {
                                // Обновляем пользователя: добавляем в историю и удаляем из планируемых
                                Update update = new Update()
                                        .addToSet("tripHistory", tripId)
                                        .pull("plannedTrips", tripId)
                                        .unset("tripInPlanning");

                                return mongoTemplate.findAndModify(query, update, UserDBO.class)
                                        .thenReturn(trip);
                            })
                            .onErrorResume(e -> {
                                logger.error("Error finishing planning for user {}: {}", chatId, e.getMessage());
                                return Mono.error(e);
                            });
                })
                .onErrorResume(e -> {
                    logger.error("Error in finishPlanning for chatId {}: {}", chatId, e.getMessage());
                    return Mono.error(e);
                });
    }

    @Override
    public Mono<Trip> cancelPlanning(Long chatId) {
        Query query = Query.query(Criteria.where("chatId").is(chatId));
        return mongoTemplate.findOne(query, UserDBO.class)
                .flatMap(user -> {
                    ObjectId tripId = user.getTripInPlanning();
                    if (tripId == null) return Mono.empty();

                    Update update = new Update().unset("tripInPlanning");

                    return mongoTemplate.findAndModify(query, update, UserDBO.class)
                            .flatMap(u -> tripDAO.getTrip(tripId.toHexString())
                                    .onErrorResume(e -> {
                                        logger.error("Error getting trip after canceling planning for user {}: {}", 
                                            chatId, e.getMessage());
                                        return Mono.empty();
                                    }));
                })
                .onErrorResume(e -> {
                    logger.error("Error in cancelPlanning for chatId {}: {}", chatId, e.getMessage());
                    return Mono.error(e);
                });
    }

    @Override
    public Mono<Trip> deletePlannedTrip(Long chatId, String tripId) {
        ObjectId tripObjectId = new ObjectId(tripId);
        Query query = Query.query(Criteria.where("chatId").is(chatId));
        Update update = new Update().pull("plannedTrips", tripObjectId);

        return mongoTemplate.findAndModify(query, update, UserDBO.class)
                .flatMap(user -> tripDAO.getTrip(tripId)
                        .onErrorResume(e -> {
                            logger.error("Error getting trip after deletion for user {}: {}", 
                                chatId, e.getMessage());
                            return Mono.empty();
                        }))
                .onErrorResume(e -> {
                    logger.error("Error in deletePlannedTrip for chatId {} and tripId {}: {}", 
                        chatId, tripId, e.getMessage());
                    return Mono.error(e);
                });
    }

    @Override
    public Flux<Trip> getCurrentTrips(Long chatId) {
        Query query = new Query(Criteria.where("chatId").is(chatId));
        return mongoTemplate.findOne(query, UserDBO.class)
                .flatMapMany(userDBO -> {
                    if (userDBO.getCurrentTrips() == null || userDBO.getCurrentTrips().isEmpty()) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(userDBO.getCurrentTrips())
                            .flatMap(tripId -> tripDAO.getTrip(tripId.toHexString())
                                    .onErrorResume(e -> {
                                        logger.error("Error getting current trip {} for user {}: {}", 
                                            tripId, chatId, e.getMessage());
                                        return Mono.empty();
                                    }));
                })
                .onErrorResume(e -> {
                    logger.error("Error in getCurrentTrips for chatId {}: {}", chatId, e.getMessage());
                    return Flux.error(e);
                });
    }

    @Override
    public Flux<Trip> getFinishedTrips(Long chatId) {
        Query query = new Query(Criteria.where("chatId").is(chatId));
        return mongoTemplate.findOne(query, UserDBO.class)
                .flatMapMany(userDBO -> {
                    if (userDBO.getTripHistory() == null || userDBO.getTripHistory().isEmpty()) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(userDBO.getTripHistory())
                            .flatMap(tripId -> tripDAO.getTrip(tripId.toHexString())
                                    .onErrorResume(e -> {
                                        logger.error("Error getting finished trip {} for user {}: {}", 
                                            tripId, chatId, e.getMessage());
                                        return Mono.empty();
                                    }));
                })
                .onErrorResume(e -> {
                    logger.error("Error in getFinishedTrips for chatId {}: {}", chatId, e.getMessage());
                    return Flux.error(e);
                });
    }

    @Override
    public Mono<Void> updateUserLocation(Long chatId, double latitude, double longitude) {
        Query query = new Query(Criteria.where("chatId").is(chatId));
        Update update = new Update()
                .set("location.latitude", latitude)
                .set("location.longitude", longitude);
        return mongoTemplate.updateFirst(query, update, UserDBO.class)
                .then()
                .onErrorResume(e -> {
                    logger.error("Error updating location for user {}: {}", chatId, e.getMessage());
                    return Mono.error(e);
                });
    }

    @Override
    public Mono<User> getUserByChatId(Long chatId) {
        Query query = Query.query(Criteria.where("chatId").is(chatId));
        return mongoTemplate.findOne(query, UserDBO.class)
                .map(userMapper::fromDbo)
                .onErrorResume(e -> {
                    logger.error("Error in getUserByChatId for chatId {}: {}", chatId, e.getMessage());
                    return Mono.error(e);
                });
    }

    @Override
    public Mono<Void> addToTripHistory(Long chatId, String tripId) {
        Query query = Query.query(Criteria.where("chatId").is(chatId));
        Update update = new Update().addToSet("tripHistory", new ObjectId(tripId));
        return mongoTemplate.updateFirst(query, update, UserDBO.class)
                .then()
                .onErrorResume(e -> {
                    logger.error("Error adding trip {} to history for user {}: {}", tripId, chatId, e.getMessage());
                    return Mono.error(e);
                });
    }

    @Override
    public Mono<Void> removeFromPlannedTrips(Long chatId, String tripId) {
        Query query = Query.query(Criteria.where("chatId").is(chatId));
        Update update = new Update().pull("plannedTrips", new ObjectId(tripId));
        return mongoTemplate.updateFirst(query, update, UserDBO.class)
                .then()
                .onErrorResume(e -> {
                    logger.error("Error removing trip {} from planned trips for user {}: {}", tripId, chatId, e.getMessage());
                    return Mono.error(e);
                });
    }

    @Override
    public Mono<Void> setOngoingTrip(Long chatId, String tripId) {
        Query query = Query.query(Criteria.where("chatId").is(chatId));
        Update update = new Update()
                .set("ongoingTrip", new ObjectId(tripId))
                .pull("plannedTrips", new ObjectId(tripId))
                .addToSet("currentTrips", new ObjectId(tripId));
        return mongoTemplate.updateFirst(query, update, UserDBO.class)
                .then()
                .onErrorResume(e -> {
                    logger.error("Error setting ongoing trip {} for user {}: {}", tripId, chatId, e.getMessage());
                    return Mono.error(e);
                });
    }

    private UserDBO newUserDbo(Long chatId) {
        UserDBO dbo = new UserDBO();
        dbo.setChatId(chatId);
        dbo.setPlannedTrips(new ArrayList<>());
        dbo.setCurrentTrips(new ArrayList<>());
        dbo.setTripHistory(new ArrayList<>());
        return dbo;
    }
}
