package com.sonrisa.alerting.spi.subscriber;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Everything that differs between kinds of subscribers (email, Slack, ...): input validation,
 * normalisation, optional verification, masking, whether the address is a secret, and the channel that
 * delivers to them. Architecture Section 7.2, ADR-11.
 *
 * <p>The application calls a subscriber type during sign-up in this order:
 * {@link #validate(SubscriberInput)}, then (only for valid input) {@link #normalise(SubscriberInput)},
 * then, for a new address, {@link #verify(NormalisedAddress, String)}. It stores the normalised address
 * (encrypted when {@link #addressIsSecret()}), a fingerprint of it for duplicate checks and the
 * {@link #mask(NormalisedAddress) masked form}.
 *
 * <p><b>Not responsible for:</b> storage, encryption, fingerprints, duplicate detection, the HTTP API,
 * and delivering notifications (that is the job of the {@link #channelKey() channel}).
 *
 * <p>Implementations must be thread-safe. They must never log an address that
 * {@link #addressIsSecret() is secret}.
 */
public interface SubscriberType {

    /**
     * The type key, for example {@code email} or {@code slack}. Lower-case letters, digits and hyphens,
     * starting with a letter. It is stored with every subscriber, so it must never change.
     */
    String key();

    /**
     * The key of the {@link com.sonrisa.alerting.spi.channel.NotificationChannel} that delivers to
     * subscribers of this type (for example {@code email} for email subscribers). The application refuses
     * to start when no enabled channel has this key.
     */
    String channelKey();

    /**
     * Whether the address is a secret (a Slack webhook URL is; an email address is not). A secret address
     * is encrypted at rest, never shown in full and never logged (NFR-04, ADR-09).
     */
    boolean addressIsSecret();

    /**
     * Checks the raw input (FR-02, FR-04 format check). Performs no network calls.
     *
     * @param input the raw input from the sign-up form; fields may be {@code null} or blank
     * @return one entry per problem, empty when the input is valid; never {@code null}. Field names are
     *     {@link SubscriberInput#FIELD_DISPLAY_NAME} and {@link SubscriberInput#FIELD_ADDRESS}.
     */
    List<FieldError> validate(SubscriberInput input);

    /**
     * Returns the canonical form of the address, so that the same address always gives the same value
     * (for example a lower-case email, or a webhook URL without trailing slash or query). Duplicate
     * detection (FR-05) relies on this.
     *
     * @param input input for which {@link #validate(SubscriberInput)} returned no errors
     * @return the normalised address
     * @throws IllegalArgumentException if the input is not valid
     */
    NormalisedAddress normalise(SubscriberInput input);

    /**
     * Optional verification of a new address before it is stored, for example a Slack welcome message
     * (FR-04). Called only for addresses that are not yet subscribed. Must use a short timeout. The
     * default performs no verification.
     *
     * @param address the normalised address
     * @param displayName the display name or label as entered, or {@code null} when none was given
     * @return the outcome; a {@link VerificationResult.Outcome#FAILED failed} verification rejects the
     *     sign-up and nothing is stored
     */
    default VerificationResult verify(NormalisedAddress address, @Nullable String displayName) {
        return VerificationResult.notRequired();
    }

    /**
     * A form of the address that is safe to show in the admin list and in logs (NFR-04). For a secret
     * address it must not reveal the secret part (for example
     * {@code https://hooks.slack.com/services/T0000/B0000/****}). A non-secret type may return the address
     * unchanged.
     */
    String mask(NormalisedAddress address);
}
