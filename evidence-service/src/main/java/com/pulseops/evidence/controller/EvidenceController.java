package com.pulseops.evidence.controller;

import com.pulseops.evidence.model.Evidence;
import com.pulseops.evidence.service.EvidenceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EvidenceController {

    private final EvidenceService evidenceService;

    @PostMapping(value = "/api/incidents/{incidentId}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Evidence> uploadEvidence(
            @PathVariable String incidentId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            HttpServletRequest request) {

        String corrId = correlationId != null ? correlationId : java.util.UUID.randomUUID().toString();
        log.info("Uploading evidence for incident: incidentId={}, filename={}, correlationId={}",
                incidentId, file.getOriginalFilename(), corrId);

        Evidence evidence = evidenceService.uploadEvidence(incidentId, file, corrId);
        return ResponseEntity.ok(evidence);
    }

    @GetMapping("/api/incidents/{incidentId}/evidence")
    public ResponseEntity<List<Evidence>> listEvidence(@PathVariable String incidentId) {
        log.debug("Listing evidence for incident: {}", incidentId);
        List<Evidence> evidenceList = evidenceService.listEvidenceForIncident(incidentId);
        return ResponseEntity.ok(evidenceList);
    }

    @GetMapping("/api/evidence/{evidenceId}/download")
    public ResponseEntity<StreamingResponseBody> downloadEvidence(@PathVariable String evidenceId) {
        log.info("Downloading evidence: {}", evidenceId);

        Evidence evidence = evidenceService.getEvidence(evidenceId);
        InputStream inputStream = evidenceService.downloadEvidence(evidenceId);

        StreamingResponseBody responseBody = outputStream -> {
            try (InputStream is = inputStream) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        };

        String contentDisposition = String.format("attachment; filename=\"%s\"", evidence.getFilename());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(evidence.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(evidence.getSizeBytes()))
                .body(responseBody);
    }
}
