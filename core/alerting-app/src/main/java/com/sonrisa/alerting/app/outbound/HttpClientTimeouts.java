package com.sonrisa.alerting.app.outbound;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;

/**
 * Connect and read timeout of one outbound HTTP integration (architecture Section 13.3, NFR-08). Meant to
 * be nested in the integration's own configuration group, for example
 * {@code alerting.sources.newsapi.http.connect-timeout}; both values are required.
 *
 * @param connectTimeout time to establish the connection
 * @param readTimeout time to wait for response data
 */
public record HttpClientTimeouts(
        @NotNull @DurationMin(millis = 1) @DurationMax(minutes = 5) Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) @DurationMax(minutes = 5) Duration readTimeout) {
}
