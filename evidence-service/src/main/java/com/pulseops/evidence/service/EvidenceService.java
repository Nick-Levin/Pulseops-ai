package com.pulseops.evidence.service;

import com.pulseops.evidence.messaging.EventPublisher;
import com.pulseops.evidence.model.Evidence;
import com.pulseops.evidence.repository.EvidenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final MinioStorageService minioStorageService;
    private final EventPublisher eventPublisher;

    public Evidence uploadEvidence(String incidentId, MultipartFile file, String correlationId) {
        try {
            String evidenceId = generateEvidenceId();
            String filename = file.getOriginalFilename();
            String objectKey = buildObjectKey(incidentId, evidenceId, filename);

            log.info("Uploading evidence: incidentId={}, evidenceId={}, filename={}, size={}",
                    incidentId, evidenceId, filename, file.getSize());

            // Upload to MinIO
            minioStorageService.uploadFile(
                    objectKey,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );

            // Save metadata to MongoDB
            Evidence evidence = Evidence.builder()
                    .id(evidenceId)
                    .incidentId(incidentId)
                    .filename(filename)
                    .contentType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .objectKey(objectKey)
                    .uploadedAt(Instant.now())
                    .build();

            Evidence savedEvidence = evidenceRepository.save(evidence);
            log.info("Evidence metadata saved: evidenceId={}", evidenceId);

            // Publish event to Kafka
            publishEvidenceUploadedEvent(savedEvidence, correlationId);

            return savedEvidence;
        } catch (IOException e) {
            log.error("Error reading file content for incidentId={}", incidentId, e);
            throw new RuntimeException("Failed to read file content: " + e.getMessage(), e);
        }
    }

    public List<Evidence> listEvidenceForIncident(String incidentId) {
        log.debug("Listing evidence for incident: {}", incidentId);
        return evidenceRepository.findByIncidentIdOrderByUploadedAtDesc(incidentId);
    }

    public Evidence getEvidence(String evidenceId) {
        log.debug("Getting evidence: {}", evidenceId);
        return evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new RuntimeException("Evidence not found: " + evidenceId));
    }

    public InputStream downloadEvidence(String evidenceId) {
        Evidence evidence = getEvidence(evidenceId);
        log.info("Downloading evidence: evidenceId={}, objectKey={}", evidenceId, evidence.getObjectKey());
        return minioStorageService.downloadFile(evidence.getObjectKey());
    }

    private String generateEvidenceId() {
        return "EV_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    private String buildObjectKey(String incidentId, String evidenceId, String filename) {
        return incidentId + "/" + evidenceId + "-" + filename;
    }

    private void publishEvidenceUploadedEvent(Evidence evidence, String correlationId) {
        Map<String, Object> payload = Map.of(
                "evidenceId", evidence.getId(),
                "filename", evidence.getFilename(),
                "contentType", evidence.getContentType(),
                "sizeBytes", evidence.getSizeBytes()
        );

        eventPublisher.publish(
                "evidence.uploaded",
                evidence.getIncidentId(),
                evidence.getId(),
                correlationId,
                payload
        );
    }
}
