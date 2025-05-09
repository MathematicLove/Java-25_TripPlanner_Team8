package org.tripplanner.modules.healthcheck;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.tripplanner.repositories.mongodb.UserDBO;

import reactor.core.publisher.Mono;

@Component
public class HealthCheckController {
    private final ReactiveMongoTemplate mongoTemplate;

    public HealthCheckController(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Mono<ServerResponse> healthCheck(ServerRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("authors", "Salimlini Aizekini, Grigorini Petrini, Michalini Martini");
        
        return mongoTemplate.findAll(UserDBO.class)
                .take(1)
                .then(ServerResponse.ok().bodyValue(response))
                .onErrorResume(e -> {
                    response.put("status", "ERROR");
                    response.put("error", e.getMessage());
                    return ServerResponse.ok().bodyValue(response);
                });
    }
} 