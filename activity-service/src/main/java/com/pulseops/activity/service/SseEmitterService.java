package com.pulseops.activity.service;

import com.pulseops.activity.messaging.EventEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class SseEmitterService {

    private final Sinks.Many<EventEnvelope> sink;

    public SseEmitterService() {
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
    }

    public Flux<EventEnvelope> subscribe() {
        String clientId = UUID.randomUUID().toString();
        log.info("New SSE client connected: {}", clientId);

        return sink.asFlux()
                .doOnSubscribe(sub -> log.debug("Client {} subscribed to SSE stream", clientId))
                .doOnCancel(() -> log.debug("Client {} disconnected from SSE stream", clientId))
                .doOnError(error -> log.error("SSE stream error for client {}: {}", clientId, error.getMessage()));
    }

    public Flux<EventEnvelope> subscribeWithHeartbeat() {
        return subscribe()
                .mergeWith(heartbeatFlux())
                .onErrorResume(error -> {
                    log.error("Error in SSE stream: {}", error.getMessage());
                    return Flux.empty();
                });
    }

    public void emit(EventEnvelope event) {
        Sinks.EmitResult result = sink.tryEmitNext(event);
        if (result.isSuccess()) {
            log.debug("Event emitted to SSE clients: type={}", event.getType());
        } else {
            log.warn("Failed to emit event to SSE clients: result={}", result);
        }
    }

    private Flux<EventEnvelope> heartbeatFlux() {
        return Flux.interval(Duration.ofSeconds(30))
                .map(tick -> EventEnvelope.builder()
                        .type("heartbeat")
                        .eventId("hb-" + System.currentTimeMillis())
                        .build());
    }
}
