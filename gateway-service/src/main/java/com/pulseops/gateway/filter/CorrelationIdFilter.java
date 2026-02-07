package com.pulseops.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Extract or generate correlation ID
        String correlationId = extractOrGenerateCorrelationId(exchange);
        
        // Store in exchange attributes for access by other filters
        exchange.getAttributes().put(CORRELATION_ID_KEY, correlationId);
        
        // Add correlation ID to request headers for downstream services
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();
        
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();
        
        // Add correlation ID to response headers BEFORE the response is committed.
        // Once the response body starts writing, headers become read-only (ReadOnlyHttpHeaders),
        // so we must set them before chain.filter() triggers downstream processing.
        mutatedExchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        
        log.debug("Request received. method={}, path={}, correlationId={}",
                mutatedRequest.getMethod(),
                mutatedRequest.getURI().getPath(),
                correlationId);
        
        return chain.filter(mutatedExchange)
                .doOnSuccess(aVoid -> log.debug("Response sent. status={}, correlationId={}",
                        mutatedExchange.getResponse().getStatusCode(),
                        correlationId));
    }

    private String extractOrGenerateCorrelationId(ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = generateCorrelationId();
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            log.debug("Using existing correlation ID from request: {}", correlationId);
        }
        
        return correlationId;
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public int getOrder() {
        // Execute early to ensure correlation ID is available for other filters
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
