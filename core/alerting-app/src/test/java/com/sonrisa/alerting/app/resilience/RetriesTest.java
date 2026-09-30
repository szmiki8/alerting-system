package com.sonrisa.alerting.app.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.functions.Either;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

/** BE-17 retry behaviour with fake calls (no Spring context, no network). */
class RetriesTest {

    static final String DETERMINISTIC = "deterministic";
    static final String JITTERED = "jittered";

    final RetryRegistry registry = RetryRegistry.of(Map.of(
            // 10 ms, 20 ms, 40 ms: growing waits without jitter, so the growth is exactly visible.
            DETERMINISTIC, RetryConfig.custom().maxAttempts(4)
                    .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(10), 2.0))
                    .build(),
            // Same shape as the configured defaults: exponential with +/- 50 % jitter.
            JITTERED, RetryConfig.custom().maxAttempts(3)
                    .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(Duration.ofMillis(100), 2.0, 0.5))
                    .build()));

    final Retries retries = new Retries(registry, List.of(new StandardFailureClassifier()), Duration.ofSeconds(5));

    /** A fake call that fails {@code failures} times with the given outcome, then returns "ok". */
    static Supplier<Object> failing(int failures, Supplier<RuntimeException> failure, AtomicInteger calls) {
        return () -> {
            if (calls.incrementAndGet() <= failures) {
                throw failure.get();
            }
            return "ok";
        };
    }

    static RuntimeException transientIo() {
        return new UncheckedIOException(new IOException("connection reset"));
    }

    static HttpClientErrorException clientError(HttpStatus status, String retryAfter) {
        HttpHeaders headers = new HttpHeaders();
        if (retryAfter != null) {
            headers.set(HttpHeaders.RETRY_AFTER, retryAfter);
        }
        return HttpClientErrorException.create(status, status.getReasonPhrase(), headers, new byte[0],
                StandardCharsets.UTF_8);
    }

    static List<Duration> recordWaits(Retry retry) {
        List<Duration> waits = new ArrayList<>();
        retry.getEventPublisher().onRetry(event -> waits.add(event.getWaitInterval()));
        return waits;
    }

    @Test
    void retriesTransientFailuresUpToMaxAttemptsWithGrowingWaits() {
        Retry retry = retries.retry("fake-source", DETERMINISTIC);
        List<Duration> waits = recordWaits(retry);
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> retry.executeSupplier(failing(10, RetriesTest::transientIo, calls)))
                .isInstanceOf(UncheckedIOException.class);

        assertThat(calls).hasValue(4);
        assertThat(waits).containsExactly(Duration.ofMillis(10), Duration.ofMillis(20), Duration.ofMillis(40));
    }

    @Test
    void succeedsWhenATransientFailureGoesAway() {
        AtomicInteger calls = new AtomicInteger();

        Object result = retries.retry("recovering", DETERMINISTIC)
                .executeSupplier(failing(2, RetriesTest::transientIo, calls));

        assertThat(result).isEqualTo("ok");
        assertThat(calls).hasValue(3);
    }

    @Test
    void doesNotRetryPermanentFailures() {
        AtomicInteger calls = new AtomicInteger();
        Retry retry = retries.retry("permanent", DETERMINISTIC);

        assertThatThrownBy(() -> retry.executeSupplier(failing(10, () -> clientError(HttpStatus.NOT_FOUND, null), calls)))
                .isInstanceOf(HttpClientErrorException.NotFound.class);
        assertThat(calls).hasValue(1);
    }

    @Test
    void doesNotRetryUnclassifiedExceptions() {
        AtomicInteger calls = new AtomicInteger();
        Retry retry = retries.retry("bug", DETERMINISTIC);

        assertThatThrownBy(() -> retry.executeSupplier(failing(10, () -> new IllegalStateException("bug"), calls)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(calls).hasValue(1);
    }

    @Test
    void retriesTransientResultsAndReturnsTheLastOneWhenAttemptsRunOut() {
        Classified transientResult = Classification::transientFailure;
        AtomicInteger calls = new AtomicInteger();

        Object result = retries.retry("result-based", DETERMINISTIC).executeSupplier(() -> {
            calls.incrementAndGet();
            return transientResult;
        });

        assertThat(calls).hasValue(4);
        assertThat(result).isSameAs(transientResult);
    }

    @Test
    void successfulResultsAreNotRetried() {
        AtomicInteger calls = new AtomicInteger();

        retries.retry("success", DETERMINISTIC).executeSupplier(() -> calls.incrementAndGet());

        assertThat(calls).hasValue(1);
    }

    @Test
    void tooManyRequestsWaitsAtLeastTheRetryAfterTime() {
        Retry retry = retries.retry("rate-limited", DETERMINISTIC);
        List<Duration> waits = recordWaits(retry);
        AtomicInteger calls = new AtomicInteger();
        List<Long> callTimes = new ArrayList<>();

        Object result = retry.executeSupplier(() -> {
            callTimes.add(System.nanoTime());
            if (calls.incrementAndGet() == 1) {
                throw clientError(HttpStatus.TOO_MANY_REQUESTS, "1");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(waits).containsExactly(Duration.ofSeconds(1));
        assertThat(Duration.ofNanos(callTimes.get(1) - callTimes.get(0))).isGreaterThanOrEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void retryAfterLongerThanTheLimitEndsTheRetries() {
        AtomicInteger calls = new AtomicInteger();
        Retry retry = retries.retry("paused", DETERMINISTIC);

        assertThatThrownBy(() -> retry.executeSupplier(
                failing(10, () -> clientError(HttpStatus.TOO_MANY_REQUESTS, "3600"), calls)))
                .isInstanceOf(HttpClientErrorException.TooManyRequests.class);
        assertThat(calls).hasValue(1);
    }

    @Test
    void jitteredBackoffGrowsAndVaries() {
        RetryConfig config = retries.retry("jitter", JITTERED).getRetryConfig();
        Either<Throwable, Object> failure = Either.left(transientIo());
        List<Long> firstWaits = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            long first = config.<Object>getIntervalBiFunction().apply(1, failure);
            long second = config.<Object>getIntervalBiFunction().apply(2, failure);
            assertThat(first).isBetween(50L, 150L);
            assertThat(second).isBetween(100L, 300L);
            firstWaits.add(first);
        }
        assertThat(firstWaits.stream().distinct().count()).as("jitter spreads the waits").isGreaterThan(5);
    }

    @Test
    void unknownBaseConfigurationIsAnError() {
        assertThatThrownBy(() -> retries.retry("orphan", "no-such-config"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no-such-config");
    }
}
