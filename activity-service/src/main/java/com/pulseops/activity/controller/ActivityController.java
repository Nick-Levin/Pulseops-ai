package com.pulseops.activity.controller;

import com.pulseops.activity.messaging.EventEnvelope;
import com.pulseops.activity.model.ActivityItem;
import com.pulseops.activity.service.ActivityService;
import com.pulseops.activity.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final SseEmitterService sseEmitterService;

    @GetMapping("/activity")
    public Flux<ActivityItem> getActivity(
            @RequestParam(required = false) String incidentId) {
        log.debug("GET /api/activity - incidentId={}", incidentId);
        
        if (incidentId != null && !incidentId.isBlank()) {
            return activityService.getActivityByIncidentId(incidentId);
        }
        return activityService.getRecentActivity();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<org.springframework.http.codec.ServerSentEvent<EventEnvelope>> streamEvents() {
        log.debug("GET /api/stream - SSE connection established");
        
        return sseEmitterService.subscribeWithHeartbeat()
                .map(event -> org.springframework.http.codec.ServerSentEvent.<EventEnvelope>builder()
                        .id(event.getEventId())
                        .event("domainEvent")
                        .data(event)
                        .build())
                .doOnCancel(() -> log.debug("SSE client disconnected"))
                .onErrorResume(e -> {
                    log.debug("SSE stream error (client likely disconnected): {}", e.getMessage());
                    return Flux.empty();
                });
    }
}
