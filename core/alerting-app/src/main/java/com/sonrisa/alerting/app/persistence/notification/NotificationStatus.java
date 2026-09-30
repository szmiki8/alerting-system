package com.sonrisa.alerting.app.persistence.notification;

/** Delivery state of one notification (Section 8.1, ADR-06). */
public enum NotificationStatus {
    /** Waiting to be sent, from {@code nextAttemptAt} on. */
    PENDING,
    /** Claimed by a worker until {@code leaseUntil}; afterwards another worker may claim it again. */
    IN_PROGRESS,
    SENT,
    /** Given up (permanent error or maximum attempts reached). */
    FAILED,
    CANCELLED
}
