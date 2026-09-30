package com.sonrisa.alerting.app.resilience;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * How a failed attempt is to be treated (architecture Sections 7.1, 7.3, 13.3): transient failures
 * (timeouts, 5xx, 429) are retried, permanent failures (other 4xx, invalid data) are not. A transient
 * failure may say how long to wait before the next attempt (HTTP 429 {@code Retry-After}).
 */
public final class Classification {

    private static final Classification PERMANENT = new Classification(false, null);
    private static final Classification TRANSIENT = new Classification(true, null);

    private final boolean transientFailure;
    private final Duration retryAfter;

    private Classification(boolean transientFailure, Duration retryAfter) {
        this.transientFailure = transientFailure;
        this.retryAfter = retryAfter;
    }

    public static Classification permanentFailure() {
        return PERMANENT;
    }

    public static Classification transientFailure() {
        return TRANSIENT;
    }

    /** A transient failure whose next attempt must not start before {@code retryAfter} has passed. */
    public static Classification transientFailure(Duration retryAfter) {
        Objects.requireNonNull(retryAfter, "retryAfter");
        return retryAfter.isNegative() || retryAfter.isZero() ? TRANSIENT : new Classification(true, retryAfter);
    }

    public boolean isTransient() {
        return transientFailure;
    }

    public Optional<Duration> retryAfter() {
        return Optional.ofNullable(retryAfter);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Classification that
                && transientFailure == that.transientFailure
                && Objects.equals(retryAfter, that.retryAfter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transientFailure, retryAfter);
    }

    @Override
    public String toString() {
        return transientFailure
                ? "transient" + (retryAfter != null ? " (retry after " + retryAfter + ")" : "")
                : "permanent";
    }
}
