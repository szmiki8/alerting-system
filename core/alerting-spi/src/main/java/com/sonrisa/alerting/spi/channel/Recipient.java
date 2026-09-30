package com.sonrisa.alerting.spi.channel;

import org.jspecify.annotations.Nullable;

/**
 * The receiver of one notification.
 *
 * <p>{@link #toString()} hides the address, because it may be a secret (a Slack webhook URL, NFR-04).
 *
 * @param subscriberType the key of the subscriber's type, for example {@code slack}; not blank
 * @param address the normalised address in plain form (decrypted when stored encrypted); not blank
 * @param displayName the subscriber's name or label, or {@code null} when none was given
 */
public record Recipient(String subscriberType, String address, @Nullable String displayName) {

    public Recipient {
        if (subscriberType == null || subscriberType.isBlank()) {
            throw new IllegalArgumentException("subscriberType must not be blank");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("address must not be blank");
        }
    }

    @Override
    public String toString() {
        return "Recipient[subscriberType=" + subscriberType + ", address=***]";
    }
}
