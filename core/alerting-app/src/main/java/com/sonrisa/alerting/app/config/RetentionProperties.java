package com.sonrisa.alerting.app.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Retention group (Section 8.4, NFR-18): how long records are kept before the nightly job deletes them.
 *
 * @param events age after which events are deleted (default 30 days)
 * @param runs age after which runs and their notifications are deleted (default 90 days)
 * @param audit age after which audit entries are deleted (default 1 year)
 */
@Validated
@ConfigurationProperties("alerting.retention")
public record RetentionProperties(
        @DefaultValue("30d") @NotNull @DurationMin(days = 1) Duration events,
        @DefaultValue("90d") @NotNull @DurationMin(days = 1) Duration runs,
        @DefaultValue("365d") @NotNull @DurationMin(days = 1) Duration audit) {
}
