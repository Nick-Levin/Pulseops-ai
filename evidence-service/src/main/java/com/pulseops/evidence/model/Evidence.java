package com.pulseops.evidence.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "evidence")
public class Evidence {

    @Id
    private String id;

    @Indexed
    private String incidentId;

    private String filename;
    private String contentType;
    private Long sizeBytes;
    private String objectKey;
    private Instant uploadedAt;
}
