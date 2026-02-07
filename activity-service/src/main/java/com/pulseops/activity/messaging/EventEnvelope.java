package com.pulseops.activity.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope {

    private String eventId;
    private String type;
    private String aggregateId;
    private Instant occurredAt;
    private String traceId;
    private Map<String, Object> payload;
}
