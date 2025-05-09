package org.tripplanner.repositories.mongodb;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.tripplanner.domain.Trip;
import org.tripplanner.domain.User;
import org.tripplanner.repositories.TripDAO;
import org.tripplanner.repositories.UserDAO;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class UserDAOImpl implements UserDAO {

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
                        .unique());
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
                    // Если произошла ошибка из-за дубликата, просто получаем существующего пользователя
                    if (e.getMessage().contains("duplicate key error")) {
                        return mongoTemplate.findOne(query, UserDBO.class)
                                .map(userMapper::fromDbo);
                    }
                    return Mono.error(e);
                });
    }

    private UserDBO newUserDbo(Long chatId) {
        UserDBO dbo = new UserDBO();
        dbo.setId(new ObjectId());
        dbo.setChatId(chatId);
        dbo.setTripInPlanning(null);
        dbo.setPlannedTrips(List.of());
        dbo.setOngoingTrip(null);
        dbo.setTripHistory(List.of());
        return dbo;
    }
    @Override
    public Flux<User> getAllUsers() {
        return mongoTemplate.findAll(UserDBO.class)
                .map(userMapper::fromDbo);
    }

    @Override
    public Flux<Trip> getAllPlannedTrips(Long chatId) {
        return mongoTemplate.findOne(
                        Query.query(Criteria.where("chatId").is(chatId)),
                        UserDBO.class
                )
                .doOnNext(user -> System.out.println("Found user: " + user.getChatId() + 
                        ", planned trips: " + (user.getPlannedTrips() != null ? user.getPlannedTrips().size() : 0)))
                .switchIfEmpty(mongoTemplate.insert(newUserDbo(chatId)))
                .flatMapMany(user -> {
                    if (user.getPlannedTrips() == null || user.getPlannedTrips().isEmpty()) {
                        System.out.println("No planned trips found for user: " + user.getChatId());
                        return Flux.empty();
                    }

                    List<String> tripIds = user.getPlannedTrips().stream()
                            .map(ObjectId::toHexString)
                            .toList();
                    
                    System.out.println("Trip IDs to fetch: " + tripIds);

                    return Flux.fromIterable(tripIds)
                            .concatMap(tripId -> {
                                System.out.println("Starting to fetch trip with ID: " + tripId);
                                return tripDAO.getTrip(tripId)
                                        .doOnNext(trip -> System.out.println("Successfully fetched trip: " + trip.getName() + " with ID: " + trip.getId()))
                                        .doOnError(e -> System.out.println("Error fetching trip " + tripId + ": " + e.getMessage()))
                                        .onErrorResume(e -> {
                                            System.out.println("Error fetching trip " + tripId + ": " + e.getMessage());
                                            return Mono.empty();
                                        });
                            })
                            .doOnNext(trip -> System.out.println("About to emit trip: " + trip.getName() + " with ID: " + trip.getId()))
                            .doOnComplete(() -> System.out.println("Finished emitting all trips"))
                            .doOnError(e -> System.out.println("Error in trip emission: " + e.getMessage()));
                })
                .doOnError(e -> System.out.println("Error in getAllPlannedTrips: " + e.getMessage()));
    }


    @Override
    public Mono<Trip> getTripInPlanning(Long chatId) {
        return mongoTemplate.findOne(
                        Query.query(Criteria.where("chatId").is(chatId)),
                        UserDBO.class
                )
                .flatMap(user -> {
                    if (user.getTripInPlanning() == null) return Mono.empty();
                    return tripDAO.getTrip(user.getTripInPlanning().toHexString());
                });
    }


    @Override
    public Mono<Trip> finishPlanning(Long chatId) {
        Query query = Query.query(Criteria.where("chatId").is(chatId));

        return mongoTemplate.findOne(query, UserDBO.class)
                .flatMap(user -> {
                    ObjectId tripId = user.getTripInPlanning();
                    if (tripId == null) return Mono.empty();

                    // Обновить пользователя: добавить в plannedTrips и сбросить tripInPlanning
                    Update update = new Update()
                            .addToSet("plannedTrips", tripId)
                            .unset("tripInPlanning");

                    return mongoTemplate.findAndModify(query, update, UserDBO.class)
                            .flatMap(updatedUser -> tripDAO.getTrip(tripId.toHexString()));
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
                            .flatMap(u -> tripDAO.getTrip(tripId.toHexString()));
                });
    }


    @Override
    public Mono<Trip> deletePlannedTrip(Long chatId, String tripId) {
        ObjectId tripObjectId = new ObjectId(tripId);
        Query query = Query.query(Criteria.where("chatId").is(chatId));
        Update update = new Update().pull("plannedTrips", tripObjectId);

        return mongoTemplate.findAndModify(query, update, UserDBO.class)
                .flatMap(user -> tripDAO.getTrip(tripId));
    }


    @Override
    public Mono<Trip> getOngoingTrip(Long chatId) {
        return mongoTemplate.findOne(
                        Query.query(Criteria.where("chatId").is(chatId)),
                        UserDBO.class
                )
                .flatMap(user -> {
                    ObjectId tripId = user.getOngoingTrip();
                    if (tripId == null) return Mono.empty();
                    return tripDAO.getTrip(tripId.toHexString());
                });
    }


    @Override
    public Flux<Trip> getFinishedTrips(Long chatId) {
        return mongoTemplate.findOne(
                        Query.query(Criteria.where("chatId").is(chatId)),
                        UserDBO.class
                )
                .flatMapMany(user -> {
                    if (user.getTripHistory() == null || user.getTripHistory().isEmpty()) {
                        return Flux.empty();
                    }

                    List<String> tripIds = user.getTripHistory().stream()
                            .map(ObjectId::toHexString)
                            .toList();

                    return Flux.fromIterable(tripIds)
                            .flatMap(tripDAO::getTrip);
                });
    }


    // Остальные методы будут реализованы позже
}
