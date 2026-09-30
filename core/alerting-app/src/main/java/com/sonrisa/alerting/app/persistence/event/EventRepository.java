package com.sonrisa.alerting.app.persistence.event;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Collected events (Section 8.1). The unique {@code eventKey} is the database guarantee against duplicates (FR-10). */
public interface EventRepository extends JpaRepository<Event, UUID> {

    boolean existsByEventKey(String eventKey);

    Optional<Event> findByEventKey(String eventKey);
}
