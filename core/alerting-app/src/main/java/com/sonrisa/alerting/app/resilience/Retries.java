package com.sonrisa.alerting.app.resilience;

import io.github.resilience4j.core.functions.Either;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Named Resilience4j retries for sources and channels (BE-17, ADR-07, architecture Section 13.3, NFR-08).
 *
 * <p><b>Configuration, without code change:</b> the shared configurations {@value #SOURCES} and
 * {@value #CHANNELS} under {@code resilience4j.retry.configs} set attempts and exponential backoff with
 * jitter. One integration can override them as an instance, for example
 * {@code resilience4j.retry.instances.newsapi.max-attempts=5} (with {@code base-config: sources}).
 * Without an instance entry, {@link #retry(String, String)} uses the named shared configuration.
 *
 * <p><b>What is retried</b> is decided here, not in the properties: the {@link FailureClassifier} beans
 * classify each thrown exception and each returned value. Only transient failures are retried; the wait
 * before the next attempt is the configured backoff, or the {@code Retry-After} time if that is longer.
 * A {@code Retry-After} above {@code alerting.resilience.max-retry-after} is not waited for: the retries
 * end and the caller gets the transient failure. {@code retry-exceptions} / {@code ignore-exceptions}
 * and result predicates in the properties are therefore not used.
 *
 * <p>The retries stay in the {@link RetryRegistry}, so Resilience4j's Micrometer binding exports their
 * metrics ({@code resilience4j.retry.calls}, tagged with the name).
 */
@Component
@EnableConfigurationProperties(ResilienceProperties.class)
public class Retries {

    /** Shared retry configuration for event sources. */
    public static final String SOURCES = "sources";
    /** Shared retry configuration for notification channels. */
    public static final String CHANNELS = "channels";

    private final RetryRegistry registry;
    private final List<FailureClassifier> classifiers;
    private final Duration maxRetryAfter;
    private final ConcurrentMap<String, Retry> retries = new ConcurrentHashMap<>();

    @Autowired
    public Retries(RetryRegistry registry, ObjectProvider<FailureClassifier> classifiers,
            ResilienceProperties properties) {
        this(registry, classifiers.orderedStream().toList(), properties.maxRetryAfter());
    }

    Retries(RetryRegistry registry, List<FailureClassifier> classifiers, Duration maxRetryAfter) {
        this.registry = registry;
        this.classifiers = List.copyOf(classifiers);
        this.maxRetryAfter = maxRetryAfter;
    }

    /**
     * The retry for one integration.
     *
     * @param name instance name, for example the source or channel key; also the metrics tag
     * @param baseConfig shared configuration to use when there is no instance entry for {@code name},
     *        usually {@link #SOURCES} or {@link #CHANNELS}
     */
    public Retry retry(String name, String baseConfig) {
        return retries.computeIfAbsent(name, key -> create(key, baseConfig));
    }

    /** The classification of one outcome (exception or returned value) by the first classifier that knows it. */
    public Optional<Classification> classify(Object outcome) {
        for (FailureClassifier classifier : classifiers) {
            Optional<Classification> classification = classifier.classify(outcome);
            if (classification.isPresent()) {
                return classification;
            }
        }
        return Optional.empty();
    }

    private Retry create(String name, String baseConfig) {
        Optional<Retry> fromInstanceProperties = registry.find(name);
        RetryConfig configured = fromInstanceProperties.map(Retry::getRetryConfig)
                .or(() -> registry.getConfiguration(baseConfig))
                .orElseThrow(() -> new IllegalArgumentException("No retry configuration '" + baseConfig
                        + "' (resilience4j.retry.configs." + baseConfig + ") for '" + name + "'"));
        RetryConfig config = RetryConfig.<Object>from(configured)
                .retryOnException(this::shouldRetry)
                .retryOnResult(this::shouldRetry)
                .intervalBiFunction((attempt, outcome) -> waitMillis(configured, attempt, outcome))
                .build();
        if (fromInstanceProperties.isPresent()) {
            Retry retry = Retry.of(name, config);
            registry.replace(name, retry);
            return retry;
        }
        return registry.retry(name, config);
    }

    private boolean shouldRetry(Object outcome) {
        return classify(outcome)
                .filter(Classification::isTransient)
                .filter(classification -> classification.retryAfter()
                        .map(wait -> wait.compareTo(maxRetryAfter) <= 0)
                        .orElse(true))
                .isPresent();
    }

    private long waitMillis(RetryConfig configured, int attempt, Either<Throwable, Object> outcome) {
        long backoff = configured.getIntervalBiFunction().apply(attempt, outcome);
        Object value = outcome.isLeft() ? outcome.getLeft() : outcome.get();
        long retryAfter = classify(value).flatMap(Classification::retryAfter).map(Duration::toMillis).orElse(0L);
        return Math.max(backoff, retryAfter);
    }
}
