package com.sonrisa.alerting.app.resilience;

import java.io.IOException;
import java.time.Clock;
import java.util.Optional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * The generic classification rules (architecture Section 13.3), asked after any more specific classifier:
 *
 * <ul>
 *   <li>{@link Classified} exceptions and results classify themselves;</li>
 *   <li>HTTP 429: transient, waiting at least the {@code Retry-After} time;</li>
 *   <li>HTTP 5xx: transient; any other HTTP error status: permanent;</li>
 *   <li>I/O problems, including connect and read timeouts ({@link ResourceAccessException}): transient.</li>
 * </ul>
 *
 * The cause chain is searched, so wrapped exceptions are recognised too.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class StandardFailureClassifier implements FailureClassifier {

    private static final int MAX_CAUSE_DEPTH = 10;

    private final Clock clock;

    public StandardFailureClassifier() {
        this(Clock.systemUTC());
    }

    StandardFailureClassifier(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Optional<Classification> classify(Object outcome) {
        if (outcome instanceof Classified classified) {
            return Optional.of(classified.classification());
        }
        Throwable current = outcome instanceof Throwable throwable ? throwable : null;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++, current = current.getCause()) {
            Optional<Classification> classification = classifyOne(current);
            if (classification.isPresent()) {
                return classification;
            }
        }
        return Optional.empty();
    }

    private Optional<Classification> classifyOne(Throwable throwable) {
        if (throwable instanceof Classified classified) {
            return Optional.of(classified.classification());
        }
        if (throwable instanceof RestClientResponseException response) {
            return Optional.of(classifyStatus(response));
        }
        if (throwable instanceof ResourceAccessException || throwable instanceof IOException) {
            return Optional.of(Classification.transientFailure());
        }
        return Optional.empty();
    }

    private Classification classifyStatus(RestClientResponseException response) {
        int status = response.getStatusCode().value();
        if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
            HttpHeaders headers = response.getResponseHeaders();
            String retryAfter = headers != null ? headers.getFirst(HttpHeaders.RETRY_AFTER) : null;
            return RetryAfter.parse(retryAfter, clock)
                    .map(Classification::transientFailure)
                    .orElseGet(Classification::transientFailure);
        }
        return response.getStatusCode().is5xxServerError()
                ? Classification.transientFailure()
                : Classification.permanentFailure();
    }
}
