/**
 * Resilience foundation (BE-17, ADR-07, architecture Section 13.3): named Resilience4j retries with
 * exponential backoff and jitter, and the transient/permanent classification that decides what is
 * retried. Rate limiters and circuit breakers follow in BE-35.
 */
package com.sonrisa.alerting.app.resilience;
