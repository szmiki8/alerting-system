package com.sonrisa.alerting.spi.subscriber;

import org.jspecify.annotations.Nullable;

/**
 * Raw sign-up input, as entered, before validation (generic subscriber model, ADR-11). The subscriber
 * type gives the fields their meaning: for email, the name and the email address; for Slack, the optional
 * label and the webhook URL. Both fields may be {@code null} or blank; {@link SubscriberType#validate}
 * reports that.
 *
 * <p>{@link #toString()} hides the address, because it may be a secret.
 *
 * @param displayName the name or label
 * @param address the email address, webhook URL or other type-specific address
 */
public record SubscriberInput(@Nullable String displayName, @Nullable String address) {

    /** Field name for {@link FieldError}s about the display name. */
    public static final String FIELD_DISPLAY_NAME = "displayName";

    /** Field name for {@link FieldError}s about the address. */
    public static final String FIELD_ADDRESS = "address";

    @Override
    public String toString() {
        return "SubscriberInput[displayName=" + displayName + ", address=" + (address == null ? "null" : "***")
                + "]";
    }
}
