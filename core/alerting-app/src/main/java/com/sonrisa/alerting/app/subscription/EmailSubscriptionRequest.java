package com.sonrisa.alerting.app.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/v1/subscriptions/email} (FR-01, FR-02). The limits mirror the {@code email} subscriber
 * type (BE-15) so that they appear in the OpenAPI document; the subscriber type stays the final authority (format,
 * trimming, control characters).
 *
 * @param name the subscriber's name
 * @param email the email address
 */
@Schema(name = "EmailSubscriptionRequest", description = "Sign-up for email notifications")
record EmailSubscriptionRequest(
        @Schema(description = "Name of the subscriber", example = "Alice Example")
        @NotBlank @Size(max = NAME_MAX_LENGTH) String name,
        @Schema(description = "Email address; stored in lower case", example = "alice@example.com")
        @NotBlank @Size(max = EMAIL_MAX_LENGTH) String email) {

    /** Same as {@code EmailSubscriberType.NAME_MAX_LENGTH}. */
    static final int NAME_MAX_LENGTH = 100;

    /** Same as {@code EmailSubscriberType.ADDRESS_MAX_LENGTH} (RFC 5321 path limit). */
    static final int EMAIL_MAX_LENGTH = 254;

    /** Hides the personal data, in case the request ever reaches a log. */
    @Override
    public String toString() {
        return "EmailSubscriptionRequest[name=***, email=***]";
    }
}
