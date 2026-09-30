package com.sonrisa.alerting.app.subscription;

import com.sonrisa.alerting.spi.subscriber.FieldError;
import java.util.List;
import java.util.Objects;

/**
 * The outcome of {@link SubscriptionService#subscribe}, free of HTTP so that each endpoint maps it itself. A new
 * and an already subscribed address give the same, equal {@link #accepted()} result (FR-05): the caller cannot
 * tell them apart.
 *
 * @param status what happened
 * @param fieldErrors the validation problems; not empty exactly when the status is {@link Status#INVALID}
 */
public record SubscriptionResult(Status status, List<FieldError> fieldErrors) {

    /** What happened to a sign-up. */
    public enum Status {
        /** The address is subscribed, either now or before (202). */
        ACCEPTED,
        /** The input is invalid; see {@link #fieldErrors()} (400 {@code validation}). Nothing was stored. */
        INVALID,
        /** The subscriber type's verification failed (problem {@code webhook-not-verified}). Nothing was stored. */
        NOT_VERIFIED,
        /** No enabled subscriber type has the given key (not found, or disabled by configuration). */
        UNKNOWN_TYPE
    }

    private static final SubscriptionResult ACCEPTED = new SubscriptionResult(Status.ACCEPTED, List.of());
    private static final SubscriptionResult NOT_VERIFIED = new SubscriptionResult(Status.NOT_VERIFIED, List.of());
    private static final SubscriptionResult UNKNOWN_TYPE = new SubscriptionResult(Status.UNKNOWN_TYPE, List.of());

    public SubscriptionResult {
        Objects.requireNonNull(status, "status");
        fieldErrors = List.copyOf(fieldErrors);
        if ((status == Status.INVALID) == fieldErrors.isEmpty()) {
            throw new IllegalArgumentException("field errors belong to, and only to, an INVALID result");
        }
    }

    public static SubscriptionResult accepted() {
        return ACCEPTED;
    }

    public static SubscriptionResult invalid(List<FieldError> fieldErrors) {
        return new SubscriptionResult(Status.INVALID, fieldErrors);
    }

    public static SubscriptionResult notVerified() {
        return NOT_VERIFIED;
    }

    public static SubscriptionResult unknownType() {
        return UNKNOWN_TYPE;
    }
}
