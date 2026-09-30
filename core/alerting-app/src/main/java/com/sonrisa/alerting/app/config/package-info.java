/**
 * Configuration groups the application owns (architecture Section 13.5), each bound to a validated
 * {@code @ConfigurationProperties} class under the prefix {@code alerting.*}. Plugin groups
 * ({@code alerting.sources.<key>}, {@code alerting.channels.<key>}) live in the plugin modules.
 * Secrets are never set in committed files; they come from environment variables or the secret store.
 */
package com.sonrisa.alerting.app.config;
