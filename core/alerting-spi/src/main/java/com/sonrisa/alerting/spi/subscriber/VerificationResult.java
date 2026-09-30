package com.sonrisa.alerting.spi.subscriber;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The outcome of {@link SubscriberType#verify}. A failed verification rejects the sign-up and nothing is
 * stored (FR-04).
 *
 * @param outcome what happened
 * @param reason why the verification failed, for the log and the diagnosis; present exactly when the
 *     outcome is {@link Outcome#FAILED}. Must not contain the address or other personal data. The
 *     application shows the user a generic message, not this reason.
 */
public record VerificationResult(Outcome outcome, @Nullable String reason) {

    /** Outcome of a verification. */
    public enum Outcome {
        /** The address was checked and works (for example Slack accepted the welcome message). */
        VERIFIED,
        /** The subscriber type has no verification step. */
        NOT_REQUIRED,
        /** The check failed (rejected by the service, time-out, connection error). */
        FAILED
    }

    public VerificationResult {
        Objects.requireNonNull(outcome, "outcome");
        if (outcome == Outcome.FAILED) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("a failed verification needs a reason");
            }
        } else if (reason != null) {
            throw new IllegalArgumentException("only a failed verification has a reason");
        }
    }

    public static VerificationResult verified() {
        return new VerificationResult(Outcome.VERIFIED, null);
    }

    public static VerificationResult notRequired() {
        return new VerificationResult(Outcome.NOT_REQUIRED, null);
    }

    public static VerificationResult failed(String reason) {
        return new VerificationResult(Outcome.FAILED, reason);
    }

    /** Whether the sign-up may go on ({@link Outcome#VERIFIED} or {@link Outcome#NOT_REQUIRED}). */
    public boolean passed() {
        return outcome != Outcome.FAILED;
    }
}
