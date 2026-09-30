package com.sonrisa.alerting.app.librarycheck;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** resilience4j-spring-boot4 auto-configures the registries from properties under Spring Boot 4.1. */
@SpringBootTest(classes = SmokeApplication.class)
class Resilience4jSmokeTest {

    @Autowired
    RetryRegistry retryRegistry;

    @Autowired
    RateLimiterRegistry rateLimiterRegistry;

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void bindsInstancesFromProperties() {
        assertThat(retryRegistry.retry("smoke").getRetryConfig().getMaxAttempts()).isEqualTo(4);
        assertThat(rateLimiterRegistry.rateLimiter("smoke").getRateLimiterConfig().getLimitForPeriod()).isEqualTo(2);
        assertThat(circuitBreakerRegistry.circuitBreaker("smoke").getCircuitBreakerConfig().getSlidingWindowSize())
                .isEqualTo(5);
    }

    @Test
    void retriesUntilSuccess() {
        Retry retry = retryRegistry.retry("smoke");
        AtomicInteger calls = new AtomicInteger();

        String result = retry.executeSupplier(() -> {
            if (calls.incrementAndGet() < 3) {
                throw new IllegalStateException("transient");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls).hasValue(3);
    }
}
