package org.tripplanner.modules.admin;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.tripplanner.repositories.mongodb.UserDBO;
import org.tripplanner.repositories.mongodb.UserMapper;

import reactor.core.publisher.Mono;

@Component
public class AdminController {
    private final ReactiveMongoTemplate mongoTemplate;
    private final UserMapper userMapper;

    public AdminController(ReactiveMongoTemplate mongoTemplate, UserMapper userMapper) {
        this.mongoTemplate = mongoTemplate;
        this.userMapper = userMapper;
    }

    public Mono<ServerResponse> getUsers(ServerRequest request) {
        return mongoTemplate.findAll(UserDBO.class)
                .map(userMapper::fromDbo)
                .collectList()
                .flatMap(users -> ServerResponse.ok().bodyValue(users));
    }
} 