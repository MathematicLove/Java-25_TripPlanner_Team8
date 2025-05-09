package org.tripplanner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.tripplanner.modules.admin.AdminController;
import org.tripplanner.modules.healthcheck.HealthCheckController;

@Configuration
@EnableWebFlux
@ComponentScan(basePackages = {
    "org.tripplanner.modules.admin",
    "org.tripplanner.modules.healthcheck"
})
public class WebConfig implements WebFluxConfigurer {

    @Bean
    public RouterFunction<ServerResponse> routes(HealthCheckController healthCheckController,
                                               AdminController adminController) {
        return RouterFunctions.route()
                .GET("/healthcheck", healthCheckController::healthCheck)
                .GET("/admin/users", adminController::getUsers)
                .build();
    }
} 