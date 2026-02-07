package com.pulseops.gateway.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class ApiKeyGatewayFilterFactory extends AbstractGatewayFilterFactory<ApiKeyGatewayFilterFactory.Config> {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    
    private final WebClient webClient;

    public ApiKeyGatewayFilterFactory(WebClient.Builder webClientBuilder,
            @Value("${secrets.service.url:http://localhost:8081}") String secretsServiceUrl) {
        super(Config.class);
        log.info("Initializing ApiKeyGatewayFilterFactory with secrets service URL: {}", secretsServiceUrl);
        this.webClient = webClientBuilder.baseUrl(secretsServiceUrl).build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Skip validation if configured to do so (e.g., for key issuance endpoint)
            if (config.isSkipValidation()) {
                return chain.filter(exchange);
            }

            String path = exchange.getRequest().getURI().getPath();
            String method = exchange.getRequest().getMethod().name();
            String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);

            // Skip validation for POST /api/keys (key issuance)
            if (path.equals("/api/keys") && method.equals("POST")) {
                log.debug("Skipping API key validation for key issuance endpoint. correlationId={}", correlationId);
                return chain.filter(exchange);
            }

            // Extract API key from header, fallback to query parameter
            String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);
            if (apiKey == null || apiKey.isBlank()) {
                apiKey = exchange.getRequest().getQueryParams().getFirst("apiKey");
            }

            if (apiKey == null || apiKey.isBlank()) {
                log.warn("API key missing. path={}, correlationId={}", path, correlationId);
                return unauthorized(exchange, "API key is required");
            }

            // Validate API key with Secrets service
            return validateApiKey(apiKey, correlationId)
                    .flatMap(valid -> {
                        if (valid) {
                            log.debug("API key validated successfully. path={}, correlationId={}", path, correlationId);
                            return chain.filter(exchange);
                        } else {
                            log.warn("Invalid API key. path={}, correlationId={}", path, correlationId);
                            return unauthorized(exchange, "Invalid API key");
                        }
                    })
                    .onErrorResume(e -> {
                        log.error("Error validating API key. path={}, correlationId={}", path, correlationId, e);
                        return unauthorized(exchange, "Unable to validate API key");
                    });
        };
    }

    private Mono<Boolean> validateApiKey(String apiKey, String correlationId) {
        return webClient.post()
                .uri("/internal/verify")
                .header(CORRELATION_ID_HEADER, correlationId != null ? correlationId : "")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new VerifyKeyRequest(apiKey))
                .exchangeToMono(response -> Mono.just(response.statusCode().is2xxSuccessful()))
                .onErrorReturn(false);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        
        String body = String.format("{\"error\":\"Unauthorized\",\"message\":\"%s\"}", message);
        byte[] bytes = body.getBytes();
        exchange.getResponse().getHeaders().setContentLength(bytes.length);
        
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.singletonList("skipValidation");
    }

    @Data
    public static class Config {
        private boolean skipValidation = false;
    }

    @Data
    private static class VerifyKeyRequest {
        private final String apiKey;
    }
}
