package com.sonrisa.alerting.app.subscription;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Body of the 202 answer to a sign-up. It is the same for a new and an already subscribed address (FR-05), so it
 * reveals nothing about existing subscriptions.
 *
 * @param message a generic confirmation text
 */
@Schema(name = "SubscriptionAcceptedResponse", description = "Generic confirmation of a sign-up")
record SubscriptionAcceptedResponse(
        @Schema(description = "Generic confirmation; identical for new and already registered addresses",
                example = MESSAGE, requiredMode = Schema.RequiredMode.REQUIRED)
        String message) {

    static final String MESSAGE = "Thank you. Your subscription has been received.";

    /** The one answer to every accepted sign-up. */
    static final SubscriptionAcceptedResponse INSTANCE = new SubscriptionAcceptedResponse(MESSAGE);
}
