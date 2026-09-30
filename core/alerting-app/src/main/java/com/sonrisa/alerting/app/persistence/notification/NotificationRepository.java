package com.sonrisa.alerting.app.persistence.notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Notifications, the delivery outbox (ADR-06). */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Notifications of a channel that a worker may take now: PENDING ones that are due, and IN_PROGRESS ones whose
     * lease has expired (their worker died or timed out). Oldest due first. Taking them (and guarding against two
     * workers taking the same row) is the delivery task's job.
     */
    @Query("""
            select n from Notification n
            where n.channelKey = :channelKey
              and ((n.status = com.sonrisa.alerting.app.persistence.notification.NotificationStatus.PENDING
                    and n.nextAttemptAt <= :now)
                or (n.status = com.sonrisa.alerting.app.persistence.notification.NotificationStatus.IN_PROGRESS
                    and n.leaseUntil < :now))
            order by n.nextAttemptAt, n.id
            """)
    List<Notification> findClaimable(@Param("channelKey") String channelKey, @Param("now") Instant now, Limit limit);
}
