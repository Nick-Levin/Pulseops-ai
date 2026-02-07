package com.pulseops.activity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "activity")
public class ActivityItem {

    @Id
    private String id;

    @Indexed
    private String type;

    @Indexed
    private String incidentId;

    @CreatedDate
    private Instant occurredAt;

    private Map<String, Object> payload;
}
