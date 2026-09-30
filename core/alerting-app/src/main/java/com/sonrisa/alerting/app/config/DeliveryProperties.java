package com.sonrisa.alerting.app.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Delivery group (Section 13.5).
 *
 * @param workerPoolSize threads per channel worker pool (bounded pools; no virtual threads on Java 17)
 * @param deadlineMargin delivery stops taking new notifications this long before the next run is due
 */
@Validated
@ConfigurationProperties("alerting.delivery")
public record DeliveryProperties(
        @DefaultValue("4") @Min(1) @Max(64) int workerPoolSize,
        @DefaultValue("5m") @NotNull @DurationMin(seconds = 0) Duration deadlineMargin) {
}
