package com.sonrisa.alerting.app.resilience;

/**
 * Implemented by application exceptions or results that know their own classification; the
 * {@link StandardFailureClassifier} uses it directly.
 */
public interface Classified {

    Classification classification();
}
