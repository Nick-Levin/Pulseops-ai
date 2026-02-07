package com.pulseops.activity.messaging;

import com.pulseops.activity.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventListener {

    private final ActivityService activityService;

    private static final Set<String> RELEVANT_EVENT_TYPES = Set.of(
            "incident.created",
            "incident.updated",
            "incident.status_changed",
            "incident.stale_detected",
            "evidence.uploaded"
    );

    @KafkaListener(
            topics = "${pulseops.kafka.topic.domain-events}",
            groupId = "${pulseops.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDomainEvent(@Payload EventEnvelope event) {
        String traceId = event.getTraceId() != null ? event.getTraceId() : "unknown";
        
        log.info("[traceId={}] Received event: type={}, aggregateId={}", 
                traceId, event.getType(), event.getAggregateId());

        if (!RELEVANT_EVENT_TYPES.contains(event.getType())) {
            log.debug("[traceId={}] Ignoring irrelevant event type: {}", traceId, event.getType());
            return;
        }

        activityService.processEvent(event)
                .doOnSuccess(activity -> log.info("[traceId={}] Activity stored: id={}, type={}", 
                        traceId, activity.getId(), activity.getType()))
                .doOnError(error -> log.error("[traceId={}] Failed to process event: {}", 
                        traceId, error.getMessage(), error))
                .subscribe();
    }
}
