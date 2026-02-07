package com.pulseops.incident.messaging;

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

    public void publish(String type, String incidentId, String correlationId, Object payload) {
        EventEnvelope event = EventEnvelope.create(type, incidentId, correlationId, payload);
        
        log.debug("Publishing event: type={}, incidentId={}, correlationId={}", 
                type, incidentId, correlationId);
        
        kafkaTemplate.send(topicName, incidentId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event: type={}, incidentId={}", 
                                type, incidentId, ex);
                    } else {
                        log.debug("Event published successfully: type={}, incidentId={}, partition={}, offset={}",
                                type, incidentId, 
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
