package com.sonrisa.alerting.app.subscription;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/v1/subscriptions/slack} (FR-03, FR-04). The limits mirror the {@code slack} subscriber
 * type (BE-18) so that they appear in the OpenAPI document; the subscriber type stays the final authority (the
 * strict webhook URL format, trimming, control characters).
 *
 * @param webhookUrl the Slack incoming webhook URL; a secret
 * @param label an optional label shown to administrators; may be absent
 */
@Schema(name = "SlackSubscriptionRequest", description = "Sign-up of a Slack channel through an incoming webhook")
record SlackSubscriptionRequest(
        @Schema(description = "Slack incoming webhook URL in the form"
                + " https://hooks.slack.com/services/T.../B.../<secret>. A secret: it is never echoed.",
                example = "https://hooks.slack.com/services/T00000000/B00000000/<secret>")
        @NotBlank @Size(max = WEBHOOK_URL_MAX_LENGTH) String webhookUrl,
        @Schema(description = "Optional label, for example the channel name", example = "#alerts",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        @Size(max = LABEL_MAX_LENGTH) String label) {

    /** Same as {@code SlackWebhookUrl.MAX_INPUT_LENGTH}. */
    static final int WEBHOOK_URL_MAX_LENGTH = 256;

    /** Same as {@code SlackSubscriberType.MAX_LABEL_LENGTH}. */
    static final int LABEL_MAX_LENGTH = 100;

    /** Hides the webhook URL (a secret) and the label, in case the request ever reaches a log. */
    @Override
    public String toString() {
        return "SlackSubscriptionRequest[webhookUrl=***, label=***]";
    }
}
