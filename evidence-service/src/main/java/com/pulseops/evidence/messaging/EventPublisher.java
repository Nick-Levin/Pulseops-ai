package com.pulseops.evidence.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;

    @Value("${pulseops.kafka.topic.domain-events:pulseops.domain-events}")
    private String topicName;

    public void publish(String type, String incidentId, String entityId, String correlationId, Object payload) {
        EventEnvelope event = EventEnvelope.create(type, incidentId, entityId, correlationId, payload);
        
        log.debug("Publishing event: type={}, incidentId={}, entityId={}, correlationId={}", 
                type, incidentId, entityId, correlationId);
        
        kafkaTemplate.send(topicName, incidentId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event: type={}, incidentId={}, entityId={}", 
                                type, incidentId, entityId, ex);
                    } else {
                        log.debug("Event published successfully: type={}, incidentId={}, entityId={}, partition={}, offset={}",
                                type, incidentId, entityId,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
