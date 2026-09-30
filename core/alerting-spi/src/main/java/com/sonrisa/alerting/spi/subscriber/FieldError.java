package com.sonrisa.alerting.spi.subscriber;

import org.jspecify.annotations.Nullable;

/**
 * One validation problem with one input field. The application returns these to the sign-up form, so
 * the message must be user-facing English and must not echo a secret address.
 *
 * @param field the field name, usually {@link SubscriberInput#FIELD_DISPLAY_NAME} or
 *     {@link SubscriberInput#FIELD_ADDRESS}; not blank
 * @param code a stable, machine-readable code, for example {@code required}, {@code invalid-format},
 *     {@code too-long}; not blank
 * @param message a short user-facing message, for example "Enter a valid email address."; not blank
 */
public record FieldError(String field, String code, String message) {

    public FieldError {
        requireText(field, "field");
        requireText(code, "code");
        requireText(message, "message");
    }

    private static void requireText(@Nullable String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
