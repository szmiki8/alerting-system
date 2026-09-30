package com.sonrisa.alerting.app.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.core.functions.Either;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * The named retry configurations from {@code application.yaml}, per-integration overrides through
 * properties only, and the Micrometer metrics (BE-17).
 */
@SpringBootTest(properties = {
        // What an operator would add to override one source without a code change:
        "resilience4j.retry.instances.tuned-source.base-config=sources",
        "resilience4j.retry.instances.tuned-source.max-attempts=5",
        "resilience4j.retry.instances.tuned-source.wait-duration=10ms"})
@ActiveProfiles("test")
class RetryConfigurationTest {

    @Autowired
    Retries retries;

    @Autowired
    MeterRegistry meterRegistry;

    static long firstWait(Retry retry) {
        RetryConfig config = retry.getRetryConfig();
        return config.<Object>getIntervalBiFunction().apply(1, Either.left(new IOException("timeout")));
    }

    @Test
    void sourcesUseTheSourcesConfiguration() {
        Retry retry = retries.retry("some-source", Retries.SOURCES);

        assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(3);
        // wait-duration 2s with +/- 50 % jitter
        assertThat(firstWait(retry)).isBetween(1_000L, 3_000L);
    }

    @Test
    void channelsUseTheChannelsConfiguration() {
        Retry retry = retries.retry("some-channel", Retries.CHANNELS);

        assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(4);
        assertThat(firstWait(retry)).isBetween(500L, 1_500L);
    }

    @Test
    void oneIntegrationCanBeTunedByPropertiesAlone() {
        Retry retry = retries.retry("tuned-source", Retries.SOURCES);
        AtomicInteger calls = new AtomicInteger();

        try {
            retry.executeRunnable(() -> {
                calls.incrementAndGet();
                throw new UncheckedIOException(new IOException("connection reset"));
            });
        } catch (UncheckedIOException expected) {
            // all attempts failed
        }

        assertThat(retry.getRetryConfig().getMaxAttempts()).isEqualTo(5);
        assertThat(calls).hasValue(5);
        assertThat(firstWait(retry)).isBetween(5L, 15L);
    }

    @Test
    void retryMetricsAreExportedThroughMicrometer() {
        retries.retry("metered-channel", Retries.CHANNELS).executeSupplier(() -> "ok");

        assertThat(meterRegistry.find("resilience4j.retry.calls").tag("name", "metered-channel").meters())
                .isNotEmpty();
    }
}
