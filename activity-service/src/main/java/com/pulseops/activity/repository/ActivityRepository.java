package com.pulseops.activity.repository;

import com.pulseops.activity.model.ActivityItem;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ActivityRepository extends ReactiveMongoRepository<ActivityItem, String> {

    Flux<ActivityItem> findTop50ByOrderByOccurredAtDesc();

    Flux<ActivityItem> findTop50ByIncidentIdOrderByOccurredAtDesc(String incidentId);
}
