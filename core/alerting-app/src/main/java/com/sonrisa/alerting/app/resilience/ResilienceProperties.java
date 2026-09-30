package com.sonrisa.alerting.app.resilience;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Application-side resilience settings. The retry policies themselves (attempts, backoff, jitter) are
 * Resilience4j configurations under {@code resilience4j.retry.*}.
 *
 * @param maxRetryAfter longest {@code Retry-After} the retry waits for inside a run. A longer one ends
 *        the retries for this call (the failure stays transient, so the caller can try again later),
 *        instead of blocking a worker thread.
 */
@Validated
@ConfigurationProperties("alerting.resilience")
public record ResilienceProperties(
        @DefaultValue("60s") @NotNull @DurationMin(seconds = 0) Duration maxRetryAfter) {
}
