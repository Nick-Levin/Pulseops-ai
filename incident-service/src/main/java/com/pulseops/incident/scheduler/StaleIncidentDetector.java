package com.pulseops.incident.scheduler;

import com.pulseops.incident.messaging.EventPublisher;
import com.pulseops.incident.model.Incident;
import com.pulseops.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaleIncidentDetector {

    private final IncidentRepository incidentRepository;
    private final EventPublisher eventPublisher;

    @Value("${pulseops.incident.stale.threshold-minutes:30}")
    private int staleThresholdMinutes;

    private static final List<Incident.IncidentStatus> ACTIVE_STATUSES = List.of(
            Incident.IncidentStatus.INVESTIGATING,
            Incident.IncidentStatus.MITIGATED
    );

    @Scheduled(fixedRateString = "${pulseops.incident.stale.check-interval-ms:300000}")
    public void detectStaleIncidents() {
        log.debug("Running stale incident detection job");

        Instant threshold = Instant.now().minus(staleThresholdMinutes, ChronoUnit.MINUTES);
        
        List<Incident> staleIncidents = incidentRepository.findStaleIncidents(ACTIVE_STATUSES, threshold);
        
        log.info("Found {} stale incidents (threshold: {} minutes)", 
                staleIncidents.size(), staleThresholdMinutes);

        for (Incident incident : staleIncidents) {
            markIncidentStale(incident);
        }
    }

    private void markIncidentStale(Incident incident) {
        try {
            incident.setStale(true);
            incident.setLastActivityAt(Instant.now());
            
            Incident saved = incidentRepository.save(incident);
            
            log.info("Marked incident as stale: id={}, status={}, lastActivityAt={}",
                    saved.getId(), saved.getStatus(), incident.getLastActivityAt());

            // Generate correlation ID for this automated action
            String correlationId = "stale-detector-" + java.util.UUID.randomUUID();
            
            eventPublisher.publish("incident.stale_detected", saved.getId(), correlationId,
                    Map.of(
                            "id", saved.getId(),
                            "status", saved.getStatus(),
                            "severity", saved.getSeverity(),
                            "assignee", saved.getAssignee(),
                            "staleSince", saved.getLastActivityAt(),
                            "detectedAt", Instant.now()
                    ));
        } catch (Exception e) {
            log.error("Failed to mark incident as stale: id={}", incident.getId(), e);
        }
    }
}
