package com.pulseops.incident.controller;

import com.pulseops.incident.model.Incident;
import com.pulseops.incident.service.IncidentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @PostMapping
    public ResponseEntity<Incident> createIncident(
            @RequestBody IncidentService.CreateIncidentRequest request,
            HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        Incident incident = incidentService.createIncident(request, correlationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(incident);
    }

    @GetMapping
    public ResponseEntity<List<Incident>> listIncidents(
            @RequestParam(required = false) Incident.IncidentStatus status,
            @RequestParam(required = false) String severity) {
        
        List<Incident> incidents = incidentService.listIncidents(status, severity);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getIncident(@PathVariable String id) {
        Optional<Incident> incident = incidentService.getIncident(id);
        return incident.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateIncident(
            @PathVariable String id,
            @RequestBody IncidentService.UpdateIncidentRequest request,
            HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        Optional<Incident> updated = incidentService.updateIncident(id, request, correlationId);
        return updated.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(
            @PathVariable String id,
            @RequestBody IncidentService.StatusChangeRequest request,
            HttpServletRequest httpRequest) {
        
        String correlationId = getCorrelationId(httpRequest);
        IncidentService.StatusChangeResult result = incidentService.changeStatus(id, request, correlationId);

        if (result.isNotFound()) {
            return ResponseEntity.notFound().build();
        }

        if (result.isInvalidTransition()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "Invalid state transition",
                            "currentStatus", result.getCurrentStatus(),
                            "requestedStatus", result.getRequestedStatus(),
                            "message", "Cannot transition from " + result.getCurrentStatus() + 
                                       " to " + result.getRequestedStatus()
                    ));
        }

        return ResponseEntity.ok(result.getIncident());
    }

    private String getCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = java.util.UUID.randomUUID().toString();
        }
        return correlationId;
    }
}
