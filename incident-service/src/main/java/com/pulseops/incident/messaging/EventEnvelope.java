package com.pulseops.incident.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope {

    private String eventId;
    private String type;
    private Instant occurredAt;
    private String producer;
    private String correlationId;
    private String entityId;
    private String incidentId;
    private Object payload;

    public static EventEnvelope create(String type, String incidentId, String correlationId, Object payload) {
        return EventEnvelope.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .type(type)
                .occurredAt(Instant.now())
                .producer("incident-service")
                .correlationId(correlationId)
                .entityId(incidentId)
                .incidentId(incidentId)
                .payload(payload)
                .build();
    }
}
