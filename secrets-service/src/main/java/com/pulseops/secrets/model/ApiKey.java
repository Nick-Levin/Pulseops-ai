package com.pulseops.secrets.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "api_keys")
public class ApiKey {

    @Id
    private String id;

    private String name;

    private String keyHash;

    private Instant createdAt;

    private Instant lastUsedAt;

    private Instant expiresAt;

    private boolean active;
}
