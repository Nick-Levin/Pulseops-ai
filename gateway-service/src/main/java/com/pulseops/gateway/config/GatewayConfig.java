package com.pulseops.gateway.config;

import com.pulseops.gateway.filter.ApiKeyGatewayFilterFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GatewayConfig {

    private final ApiKeyGatewayFilterFactory apiKeyFilterFactory;

    @Value("${secrets.service.url:http://localhost:8081}")
    private String secretsServiceUrl;

    @Value("${incident.service.url:http://localhost:8082}")
    private String incidentServiceUrl;

    @Value("${evidence.service.url:http://localhost:8083}")
    private String evidenceServiceUrl;

    @Value("${activity.service.url:http://localhost:8084}")
    private String activityServiceUrl;

    public GatewayConfig(ApiKeyGatewayFilterFactory apiKeyFilterFactory) {
        this.apiKeyFilterFactory = apiKeyFilterFactory;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        log.info("Configuring gateway routes");
        log.info("Secrets Service URL: {}", secretsServiceUrl);
        log.info("Incident Service URL: {}", incidentServiceUrl);
        log.info("Evidence Service URL: {}", evidenceServiceUrl);
        log.info("Activity Service URL: {}", activityServiceUrl);
        
        // Config for routes that require API key validation
        ApiKeyGatewayFilterFactory.Config requireApiKeyConfig = new ApiKeyGatewayFilterFactory.Config();
        requireApiKeyConfig.setSkipValidation(false);
        
        // Config for routes that skip API key validation (key issuance)
        ApiKeyGatewayFilterFactory.Config skipApiKeyConfig = new ApiKeyGatewayFilterFactory.Config();
        skipApiKeyConfig.setSkipValidation(true);
        
        return builder.routes()
                // Evidence by incident - must come BEFORE incident-service routes
                .route("evidence-by-incident", r -> r
                        .path("/api/incidents/*/evidence", "/api/incidents/*/evidence/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(apiKeyFilterFactory.apply(requireApiKeyConfig)))
                        .uri(evidenceServiceUrl))
                
                // Incident Service Routes - API Key Required
                .route("incident-service", r -> r
                        .path("/api/incidents/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(apiKeyFilterFactory.apply(requireApiKeyConfig)))
                        .uri(incidentServiceUrl))
                
                // Evidence Service Routes - API Key Required
                .route("evidence-service", r -> r
                        .path("/api/evidence/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(apiKeyFilterFactory.apply(requireApiKeyConfig)))
                        .uri(evidenceServiceUrl))
                
                // Activity Service Routes - API Key Required
                .route("activity-service", r -> r
                        .path("/api/activity/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(apiKeyFilterFactory.apply(requireApiKeyConfig)))
                        .uri(activityServiceUrl))
                
                // Activity Service SSE Stream - API Key Required
                .route("activity-stream", r -> r
                        .path("/api/stream")
                        .filters(f -> f
                                .filter(apiKeyFilterFactory.apply(requireApiKeyConfig)))
                        .uri(activityServiceUrl))
                
                // Secrets Service - Key Issuance (POST /api/keys) - No API Key Required
                .route("secrets-service-keys", r -> r
                        .path("/api/keys")
                        .and().method("POST")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(apiKeyFilterFactory.apply(skipApiKeyConfig)))
                        .uri(secretsServiceUrl))
                
                // Secrets Service - List Keys (GET /api/keys) - No API Key Required (returns metadata only)
                .route("secrets-service-keys-list", r -> r
                        .path("/api/keys")
                        .and().method("GET")
                        .filters(f -> f
                                .filter(apiKeyFilterFactory.apply(skipApiKeyConfig)))
                        .uri(secretsServiceUrl))
                
                // Secrets Service - Other Key Routes - API Key Required
                .route("secrets-service-keys-other", r -> r
                        .path("/api/keys/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(apiKeyFilterFactory.apply(requireApiKeyConfig)))
                        .uri(secretsServiceUrl))
                
                // Secrets Service - Internal Routes - API Key Required
                .route("secrets-service-internal", r -> r
                        .path("/internal/**")
                        .filters(f -> f
                                .stripPrefix(0)
                                .filter(apiKeyFilterFactory.apply(requireApiKeyConfig)))
                        .uri(secretsServiceUrl))
                
                .build();
    }
}
