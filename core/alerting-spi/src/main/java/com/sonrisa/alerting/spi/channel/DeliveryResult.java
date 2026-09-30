package com.sonrisa.alerting.spi.channel;

import java.time.Duration;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The result of one {@link NotificationChannel#send} call. Use the factory methods.
 *
 * @param outcome delivered, transient failure or permanent failure
 * @param reason why delivery failed, for example "HTTP 503" or "Slack: no_service"; present exactly for
 *     failures. It is stored and logged, so it must not contain the address or other personal data.
 * @param retryAfter for a transient failure only: the earliest time to try again that the service asked
 *     for (for example an HTTP 429 {@code Retry-After}); positive, or {@code null} to let the application
 *     choose its back-off
 */
public record DeliveryResult(Outcome outcome, @Nullable String reason, @Nullable Duration retryAfter) {

    /** How a send attempt ended. */
    public enum Outcome {
        /** The service accepted the message. */
        DELIVERED,
        /** Temporary problem (time-out, HTTP 5xx, rate limit); the application may retry. */
        TRANSIENT_FAILURE,
        /** The address no longer works (revoked webhook, channel deleted); do not retry (FR-21). */
        PERMANENT_FAILURE
    }

    public DeliveryResult {
        Objects.requireNonNull(outcome, "outcome");
        if (outcome == Outcome.DELIVERED) {
            if (reason != null) {
                throw new IllegalArgumentException("a delivered result has no reason");
            }
        } else if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("a failed result needs a reason");
        }
        if (retryAfter != null) {
            if (outcome != Outcome.TRANSIENT_FAILURE) {
                throw new IllegalArgumentException("only a transient failure has a retry-after time");
            }
            if (retryAfter.isNegative() || retryAfter.isZero()) {
                throw new IllegalArgumentException("retryAfter must be positive");
            }
        }
    }

    public static DeliveryResult delivered() {
        return new DeliveryResult(Outcome.DELIVERED, null, null);
    }

    public static DeliveryResult transientFailure(String reason) {
        return new DeliveryResult(Outcome.TRANSIENT_FAILURE, reason, null);
    }

    public static DeliveryResult transientFailure(String reason, Duration retryAfter) {
        return new DeliveryResult(Outcome.TRANSIENT_FAILURE, reason, Objects.requireNonNull(retryAfter,
                "retryAfter"));
    }

    public static DeliveryResult permanentFailure(String reason) {
        return new DeliveryResult(Outcome.PERMANENT_FAILURE, reason, null);
    }

    public boolean isDelivered() {
        return outcome == Outcome.DELIVERED;
    }
}
