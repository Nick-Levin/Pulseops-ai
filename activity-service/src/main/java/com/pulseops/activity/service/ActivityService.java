package com.pulseops.activity.service;

import com.pulseops.activity.messaging.EventEnvelope;
import com.pulseops.activity.model.ActivityItem;
import com.pulseops.activity.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final SseEmitterService sseEmitterService;

    public Mono<ActivityItem> processEvent(EventEnvelope event) {
        ActivityItem activity = ActivityItem.builder()
                .type(event.getType())
                .incidentId(event.getAggregateId())
                .payload(event.getPayload())
                .build();

        return activityRepository.save(activity)
                .doOnNext(savedActivity -> {
                    log.debug("Activity saved: id={}, type={}", savedActivity.getId(), savedActivity.getType());
                    sseEmitterService.emit(event);
                });
    }

    public Flux<ActivityItem> getRecentActivity() {
        log.debug("Fetching recent activity (last 50 events)");
        return activityRepository.findTop50ByOrderByOccurredAtDesc();
    }

    public Flux<ActivityItem> getActivityByIncidentId(String incidentId) {
        log.debug("Fetching activity for incident: {}", incidentId);
        return activityRepository.findTop50ByIncidentIdOrderByOccurredAtDesc(incidentId);
    }
}
