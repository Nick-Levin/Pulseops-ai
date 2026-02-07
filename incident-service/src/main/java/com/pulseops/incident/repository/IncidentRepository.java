package com.pulseops.incident.repository;

import com.pulseops.incident.model.Incident;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface IncidentRepository extends MongoRepository<Incident, String> {

    List<Incident> findByStatus(Incident.IncidentStatus status);

    List<Incident> findBySeverity(String severity);

    List<Incident> findByStatusAndSeverity(Incident.IncidentStatus status, String severity);

    @Query("{ 'status': { $in: ?0 }, 'lastActivityAt': { $lt: ?1 }, 'stale': false }")
    List<Incident> findStaleIncidents(List<Incident.IncidentStatus> statuses, Instant threshold);
}
