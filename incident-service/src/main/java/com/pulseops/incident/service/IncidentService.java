package com.pulseops.incident.service;

import com.pulseops.incident.messaging.EventPublisher;
import com.pulseops.incident.model.Incident;
import com.pulseops.incident.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final EventPublisher eventPublisher;

    // State machine transitions
    private static final Map<Incident.IncidentStatus, Set<Incident.IncidentStatus>> VALID_TRANSITIONS = Map.of(
            Incident.IncidentStatus.OPEN, Set.of(Incident.IncidentStatus.INVESTIGATING),
            Incident.IncidentStatus.INVESTIGATING, Set.of(
                    Incident.IncidentStatus.MITIGATED,
                    Incident.IncidentStatus.OPEN  // Allow reopen
            ),
            Incident.IncidentStatus.MITIGATED, Set.of(
                    Incident.IncidentStatus.CLOSED,
                    Incident.IncidentStatus.INVESTIGATING  // Allow back to investigating
            ),
            Incident.IncidentStatus.CLOSED, Set.of()  // Terminal state
    );

    public Incident createIncident(CreateIncidentRequest request, String correlationId) {
        Incident incident = Incident.builder()
                .id(generateIncidentId())
                .title(request.getTitle())
                .description(request.getDescription())
                .severity(request.getSeverity())
                .assignee(request.getAssignee())
                .tags(request.getTags())
                .status(Incident.IncidentStatus.OPEN)
                .stale(false)
                .lastActivityAt(Instant.now())
                .build();

        Incident saved = incidentRepository.save(incident);
        log.info("Created incident: id={}, title={}, correlationId={}", 
                saved.getId(), saved.getTitle(), correlationId);

        Map<String, Object> createdPayload = new HashMap<>();
        createdPayload.put("id", saved.getId());
        createdPayload.put("title", saved.getTitle());
        createdPayload.put("severity", saved.getSeverity());
        createdPayload.put("status", saved.getStatus());
        createdPayload.put("assignee", saved.getAssignee());
        eventPublisher.publish("incident.created", saved.getId(), correlationId, createdPayload);

        return saved;
    }

    public List<Incident> listIncidents(Incident.IncidentStatus status, String severity) {
        if (status != null && severity != null) {
            return incidentRepository.findByStatusAndSeverity(status, severity);
        } else if (status != null) {
            return incidentRepository.findByStatus(status);
        } else if (severity != null) {
            return incidentRepository.findBySeverity(severity);
        } else {
            return incidentRepository.findAll();
        }
    }

    public Optional<Incident> getIncident(String id) {
        return incidentRepository.findById(id);
    }

    public Optional<Incident> updateIncident(String id, UpdateIncidentRequest request, String correlationId) {
        return incidentRepository.findById(id).map(incident -> {
            if (request.getTitle() != null) {
                incident.setTitle(request.getTitle());
            }
            if (request.getDescription() != null) {
                incident.setDescription(request.getDescription());
            }
            if (request.getSeverity() != null) {
                incident.setSeverity(request.getSeverity());
            }
            if (request.getAssignee() != null) {
                incident.setAssignee(request.getAssignee());
            }
            incident.setLastActivityAt(Instant.now());

            Incident saved = incidentRepository.save(incident);
            log.info("Updated incident: id={}, correlationId={}", saved.getId(), correlationId);

            Map<String, Object> updatedPayload = new HashMap<>();
            updatedPayload.put("id", saved.getId());
            updatedPayload.put("title", saved.getTitle());
            updatedPayload.put("severity", saved.getSeverity());
            updatedPayload.put("status", saved.getStatus());
            updatedPayload.put("assignee", saved.getAssignee());
            eventPublisher.publish("incident.updated", saved.getId(), correlationId, updatedPayload);

            return saved;
        });
    }

    public StatusChangeResult changeStatus(String id, StatusChangeRequest request, String correlationId) {
        Optional<Incident> incidentOpt = incidentRepository.findById(id);
        if (incidentOpt.isEmpty()) {
            return StatusChangeResult.notFound();
        }

        Incident incident = incidentOpt.get();
        Incident.IncidentStatus currentStatus = incident.getStatus();
        Incident.IncidentStatus newStatus = request.getStatus();

        // Validate transition
        if (!isValidTransition(currentStatus, newStatus)) {
            log.warn("Invalid status transition: id={}, from={}, to={}, correlationId={}",
                    id, currentStatus, newStatus, correlationId);
            return StatusChangeResult.invalidTransition(currentStatus, newStatus);
        }

        incident.setStatus(newStatus);
        incident.setLastActivityAt(Instant.now());

        Incident saved = incidentRepository.save(incident);
        log.info("Status changed: id={}, from={}, to={}, correlationId={}",
                saved.getId(), currentStatus, newStatus, correlationId);

        Map<String, Object> statusPayload = new HashMap<>();
        statusPayload.put("id", saved.getId());
        statusPayload.put("previousStatus", currentStatus);
        statusPayload.put("newStatus", newStatus);
        statusPayload.put("reason", request.getReason());
        statusPayload.put("changedBy", request.getChangedBy());
        eventPublisher.publish("incident.status_changed", saved.getId(), correlationId, statusPayload);

        return StatusChangeResult.success(saved);
    }

    private boolean isValidTransition(Incident.IncidentStatus from, Incident.IncidentStatus to) {
        if (from == to) {
            return true; // Same status is always valid (no-op)
        }
        Set<Incident.IncidentStatus> allowedTransitions = VALID_TRANSITIONS.get(from);
        return allowedTransitions != null && allowedTransitions.contains(to);
    }

    private String generateIncidentId() {
        // Generate ULID-like ID: INC_ + timestamp + random
        String timestamp = Long.toString(Instant.now().toEpochMilli(), 36).toUpperCase();
        String random = Long.toString((long) (Math.random() * 1_000_000_000L), 36).toUpperCase();
        return "INC_" + timestamp + random;
    }

    // DTOs
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreateIncidentRequest {
        private String title;
        private String description;
        private String severity;
        private String assignee;
        private List<String> tags;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UpdateIncidentRequest {
        private String title;
        private String description;
        private String severity;
        private String assignee;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class StatusChangeRequest {
        private Incident.IncidentStatus status;
        private String reason;
        private String changedBy;
    }

    // Result type for status change
    @lombok.Data
    @lombok.AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    public static class StatusChangeResult {
        private final boolean success;
        private final boolean notFound;
        private final boolean invalidTransition;
        private final Incident incident;
        private final Incident.IncidentStatus currentStatus;
        private final Incident.IncidentStatus requestedStatus;

        public static StatusChangeResult success(Incident incident) {
            return new StatusChangeResult(true, false, false, incident, null, null);
        }

        public static StatusChangeResult notFound() {
            return new StatusChangeResult(false, true, false, null, null, null);
        }

        public static StatusChangeResult invalidTransition(Incident.IncidentStatus current, Incident.IncidentStatus requested) {
            return new StatusChangeResult(false, false, true, null, current, requested);
        }
    }
}
