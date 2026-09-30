/**
 * Conventions shared by all {@code /api/v1} endpoints (BE-12, architecture Section 9.2): RFC 9457
 * Problem Details with stable {@link com.sonrisa.alerting.app.api.ProblemType problem types}, field-level
 * validation errors, time values as ISO-8601 with the offset of the configured zone, and the OpenAPI
 * document (ADR-13).
 */
package com.sonrisa.alerting.app.api;
