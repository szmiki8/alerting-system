package com.sonrisa.alerting.app.persistence.notification;

import com.sonrisa.alerting.app.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One message for one event and one subscriber (database outbox, ADR-06, Section 8.1). The pair
 * ({@code eventId}, {@code subscriberId}) is unique (FR-20). {@code subscriberId} is an opaque id without a foreign
 * key, so the history survives the deletion of the subscriber (Section 8.2).
 */
@Entity
@Table(name = "notification")
public class Notification extends AssignedIdEntity {

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "subscriber_id", nullable = false, updatable = false)
    private UUID subscriberId;

    @Column(name = "run_id", nullable = false, updatable = false)
    private UUID runId;

    @Column(name = "channel_key", nullable = false, updatable = false, length = 64)
    private String channelKey;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 16)
    private NotificationStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "lease_until")
    private Instant leaseUntil;

    @Column(name = "error_reason", length = 1000)
    private String errorReason;

    @Column(name = "sent_at")
    private Instant sentAt;

    /** Not in Section 8.1: the retention job deletes notifications by this time (Section 8.4). */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** For JPA only. */
    protected Notification() {
    }

    /** A new notification, due immediately. */
    public Notification(UUID id, UUID eventId, UUID subscriberId, UUID runId, String channelKey, Instant createdAt) {
        super(id);
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.subscriberId = Objects.requireNonNull(subscriberId, "subscriberId");
        this.runId = Objects.requireNonNull(runId, "runId");
        this.channelKey = Objects.requireNonNull(channelKey, "channelKey");
        this.status = NotificationStatus.PENDING;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.nextAttemptAt = createdAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getSubscriberId() {
        return subscriberId;
    }

    public UUID getRunId() {
        return runId;
    }

    public String getChannelKey() {
        return channelKey;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getLeaseUntil() {
        return leaseUntil;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** A worker takes the notification until {@code until}; counts as one attempt. */
    public void claim(Instant until) {
        this.status = NotificationStatus.IN_PROGRESS;
        this.leaseUntil = Objects.requireNonNull(until, "until");
        this.attempts++;
    }

    public void markSent(Instant at) {
        this.status = NotificationStatus.SENT;
        this.sentAt = Objects.requireNonNull(at, "at");
        this.leaseUntil = null;
        this.errorReason = null;
    }

    /** Back to PENDING after a transient error. The reason must not contain personal data or secrets. */
    public void scheduleRetry(Instant nextAttempt, String reason) {
        this.status = NotificationStatus.PENDING;
        this.nextAttemptAt = Objects.requireNonNull(nextAttempt, "nextAttempt");
        this.leaseUntil = null;
        this.errorReason = reason;
    }

    /** Gives up. The reason must not contain personal data or secrets. */
    public void markFailed(String reason) {
        this.status = NotificationStatus.FAILED;
        this.leaseUntil = null;
        this.errorReason = reason;
    }
}
