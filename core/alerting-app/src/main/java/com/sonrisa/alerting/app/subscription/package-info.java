/**
 * Public sign-up (architecture Sections 6.1, 10.1, 10.2): the {@link com.sonrisa.alerting.app.subscription.SubscriptionService}
 * delegates validation, normalisation and verification to the matching subscriber type and enforces one
 * subscription per address through the address fingerprint (FR-05, ADR-09, ADR-11). The REST endpoints map its
 * {@link com.sonrisa.alerting.app.subscription.SubscriptionResult} to HTTP.
 */
package com.sonrisa.alerting.app.subscription;
