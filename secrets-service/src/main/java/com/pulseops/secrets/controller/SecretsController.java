package com.pulseops.secrets.controller;

import com.pulseops.secrets.service.ApiKeyService;
import com.pulseops.secrets.service.ApiKeyService.ApiKeyCreationResult;
import com.pulseops.secrets.service.ApiKeyService.ApiKeyMetadata;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SecretsController {

    private final ApiKeyService apiKeyService;

    @PostMapping("/api/keys")
    public ResponseEntity<CreateKeyResponse> createApiKey(@Valid @RequestBody CreateKeyRequest request) {
        log.info("POST /api/keys - Creating API key with name: {}", request.name());

        ApiKeyCreationResult result = apiKeyService.createApiKey(request.name());

        CreateKeyResponse response = new CreateKeyResponse(
                result.apiKey(),
                result.id(),
                result.expiresAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/keys")
    public ResponseEntity<List<ApiKeyMetadata>> listApiKeys() {
        log.info("GET /api/keys - Listing all API keys");

        List<ApiKeyMetadata> keys = apiKeyService.listApiKeys();

        return ResponseEntity.ok(keys);
    }

    @PostMapping("/internal/verify")
    public ResponseEntity<Void> verifyApiKey(@Valid @RequestBody VerifyKeyRequest request) {
        log.info("POST /internal/verify - Validating API key");

        boolean isValid = apiKeyService.validateApiKey(request.apiKey());

        if (isValid) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    public record CreateKeyRequest(@NotBlank String name) {
    }

    public record CreateKeyResponse(String apiKey, String id, Instant expiresAt) {
    }

    public record VerifyKeyRequest(@NotBlank String apiKey) {
    }
}
