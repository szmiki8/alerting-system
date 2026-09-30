package com.sonrisa.alerting.app.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.ZoneId;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Schedule group (Section 13.2, 13.5): when the collection run and the retention job start, in which
 * zone, and how long the cluster-wide lock is held.
 *
 * @param collectionCron cron of the collection run (FR-31); default: top of every hour
 * @param zone zone for the crons and for rendering times (CON-10, ASM-03); default Europe/Budapest
 * @param retentionCron cron of the nightly retention job; default 02:30
 * @param lockAtMostFor longest time a run holds the lock; slightly shorter than the interval (NFR-20)
 * @param lockAtLeastFor shortest time a run holds the lock, against clock skew between instances
 */
@Validated
@ConfigurationProperties("alerting.schedule")
public record ScheduleProperties(
        @DefaultValue("0 0 * * * *") @NotBlank @ValidCron String collectionCron,
        @DefaultValue("Europe/Budapest") @NotNull ZoneId zone,
        @DefaultValue("0 30 2 * * *") @NotBlank @ValidCron String retentionCron,
        @DefaultValue("55m") @NotNull @DurationMin(seconds = 1) Duration lockAtMostFor,
        @DefaultValue("1m") @NotNull @DurationMin(seconds = 0) Duration lockAtLeastFor) {
}
