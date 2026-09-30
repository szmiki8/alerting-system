package com.sonrisa.alerting.spi.channel;

import java.time.Duration;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The send limits of a channel, which the application's delivery service enforces (Section 7.3). For
 * example email: a global rate equal to the email service's send rate; Slack: at most one message per
 * second per webhook.
 *
 * @param global the maximum rate over all recipients of this channel, or {@code null} for no limit
 * @param perRecipient the maximum rate for one recipient address, or {@code null} for no limit
 */
public record PacingLimits(@Nullable Rate global, @Nullable Rate perRecipient) {

    /** No limits (for example the log channel). */
    public static PacingLimits unlimited() {
        return new PacingLimits(null, null);
    }

    /**
     * A rate: at most {@code permits} sends per {@code period}.
     *
     * @param permits at least 1
     * @param period positive
     */
    public record Rate(int permits, Duration period) {

        public Rate {
            if (permits < 1) {
                throw new IllegalArgumentException("permits must be at least 1, was " + permits);
            }
            Objects.requireNonNull(period, "period");
            if (period.isNegative() || period.isZero()) {
                throw new IllegalArgumentException("period must be positive");
            }
        }

        /** At most {@code permits} sends per second. */
        public static Rate perSecond(int permits) {
            return new Rate(permits, Duration.ofSeconds(1));
        }
    }
}
