package com.sonrisa.alerting.channel.slack;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration group {@code alerting.channels.slack} (architecture Section 13.5, ADR-10, OP-18).
 *
 * @param enabled switches the Slack plugin on; off unless set (OP-22). Read by the auto-configuration's
 *     condition; listed here so that it is documented in the configuration metadata
 * @param verification timeouts of the welcome message sent at sign-up (FR-04); short, because the visitor
 *     waits for the answer
 */
@Validated
@ConfigurationProperties("alerting.channels.slack")
public record SlackProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue @Valid @NotNull Timeouts verification) {

    /**
     * Connect and read timeout of one Slack webhook client.
     *
     * @param connectTimeout time to establish the connection
     * @param readTimeout time to wait for Slack's answer
     */
    public record Timeouts(
            @DefaultValue("2s") @NotNull @DurationMin(millis = 1) @DurationMax(seconds = 30) Duration connectTimeout,
            @DefaultValue("5s") @NotNull @DurationMin(millis = 1) @DurationMax(seconds = 30) Duration readTimeout) {
    }
}
