package com.pulseops.secrets.service;

import com.pulseops.secrets.model.ApiKey;
import com.pulseops.secrets.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String KEY_PREFIX = "pk_";
    private static final int KEY_BYTES_LENGTH = 32;
    private static final long DEFAULT_EXPIRY_DAYS = 90;
    private static final String HASH_ALGORITHM = "SHA-256";

    private final ApiKeyRepository apiKeyRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyCreationResult createApiKey(String name) {
        log.info("Creating new API key with name: {}", name);

        String plainKey = generatePlainApiKey();
        String keyHash = hashApiKey(plainKey);
        String keyId = generateKeyId();

        Instant now = Instant.now();
        Instant expiresAt = now.plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS);

        ApiKey apiKey = ApiKey.builder()
                .id(keyId)
                .name(name)
                .keyHash(keyHash)
                .createdAt(now)
                .expiresAt(expiresAt)
                .active(true)
                .build();

        apiKeyRepository.save(apiKey);

        log.info("API key created successfully with id: {}", keyId);

        return new ApiKeyCreationResult(plainKey, keyId, expiresAt);
    }

    public List<ApiKeyMetadata> listApiKeys() {
        log.info("Listing all API keys metadata");

        return apiKeyRepository.findAll().stream()
                .map(this::toMetadata)
                .toList();
    }

    public boolean validateApiKey(String plainKey) {
        String traceId = getTraceId();
        log.info("Validating API key, traceId: {}", traceId);

        if (plainKey == null || plainKey.isBlank()) {
            log.warn("API key validation failed: empty key provided, traceId: {}", traceId);
            return false;
        }

        String keyHash = hashApiKey(plainKey);
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyHash(keyHash);

        if (apiKeyOpt.isEmpty()) {
            log.warn("API key validation failed: key not found, traceId: {}", traceId);
            return false;
        }

        ApiKey apiKey = apiKeyOpt.get();

        if (!apiKey.isActive()) {
            log.warn("API key validation failed: key is inactive, keyId: {}, traceId: {}", apiKey.getId(), traceId);
            return false;
        }

        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(Instant.now())) {
            log.warn("API key validation failed: key has expired, keyId: {}, traceId: {}", apiKey.getId(), traceId);
            return false;
        }

        apiKey.setLastUsedAt(Instant.now());
        apiKeyRepository.save(apiKey);

        log.info("API key validated successfully, keyId: {}, traceId: {}", apiKey.getId(), traceId);
        return true;
    }

    private String generatePlainApiKey() {
        byte[] randomBytes = new byte[KEY_BYTES_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String base64Part = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return KEY_PREFIX + base64Part;
    }

    private String generateKeyId() {
        return "KEY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    private String hashApiKey(String plainKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(plainKey.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private ApiKeyMetadata toMetadata(ApiKey apiKey) {
        return new ApiKeyMetadata(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getCreatedAt(),
                apiKey.getLastUsedAt(),
                apiKey.getExpiresAt(),
                apiKey.isActive()
        );
    }

    private String getTraceId() {
        io.opentelemetry.api.trace.Span currentSpan = io.opentelemetry.api.trace.Span.current();
        if (currentSpan != null) {
            String traceId = currentSpan.getSpanContext().getTraceId();
            return traceId != null && !traceId.equals("00000000000000000000000000000000") ? traceId : "none";
        }
        return "none";
    }

    public record ApiKeyCreationResult(String apiKey, String id, Instant expiresAt) {
    }

    public record ApiKeyMetadata(String id, String name, Instant createdAt, Instant lastUsedAt,
                                  Instant expiresAt, boolean active) {
    }
}
