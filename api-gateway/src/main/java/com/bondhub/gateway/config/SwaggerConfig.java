package com.bondhub.gateway.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Swagger/OpenAPI configuration for API Gateway
 * Aggregates API documentation from all microservices
 */
@Configuration
public class SwaggerConfig {

    private final RouteDefinitionLocator routeLocator;

    public SwaggerConfig(RouteDefinitionLocator routeLocator) {
        this.routeLocator = routeLocator;
    }

    @Bean
    public List<GroupedOpenApi> apis() {
        List<GroupedOpenApi> groups = new ArrayList<>();
        
        // Get all route definitions from the gateway
        List<RouteDefinition> definitions = routeLocator.getRouteDefinitions().collectList().block();
        
        if (definitions != null) {
            definitions.stream()
                .filter(routeDefinition -> routeDefinition.getId().matches(".*(auth|user|message|notification|file)-service.*"))
                .forEach(routeDefinition -> {
                    String name = routeDefinition.getId().replaceAll("-service.*", "");
                    groups.add(GroupedOpenApi.builder()
                        .group(name)
                        .pathsToMatch("/" + name + "/**")
                        .build());
                });
        }
        
        return groups;
    }
}
