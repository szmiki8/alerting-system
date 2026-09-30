package com.sonrisa.alerting.app.resilience;

import java.util.Optional;

/**
 * Classifies the outcome of one attempt: an exception thrown by the call or the value it returned.
 * Every classifier bean is asked in {@code @Order}; the first non-empty answer wins. An empty answer for
 * all classifiers means "success" for a returned value and "permanent" for an exception, so neither is
 * retried.
 *
 * <p>This is the connection point for the SPI result types (BE-10): a classifier that maps, for example,
 * a channel's "transient failure" result (with its optional retry-after) to a {@link Classification}.
 */
@FunctionalInterface
public interface FailureClassifier {

    Optional<Classification> classify(Object outcome);
}
