package com.sonrisa.alerting.app.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Security group (Section 13.1, 13.5). Named with a prefix to avoid confusion with Spring Boot's own
 * {@code SecurityProperties}. The Google client registration stays in Spring's
 * {@code spring.security.oauth2.client.*} keys (BE-20).
 *
 * <p>The two keys are secrets: set them only through the environment
 * ({@code ALERTING_SECURITY_ENCRYPTION_KEY}, {@code ALERTING_SECURITY_FINGERPRINT_KEY}) or the secret store.
 * They are optional until BE-09 starts using them. {@link #toString()} never prints them.
 *
 * @param adminAllowList email addresses allowed into the admin area (FR-24), compared case-insensitively
 * @param sessionTimeout admin session inactivity timeout (NFR-02), default 30 minutes
 * @param encryptionKey key for encrypting webhook URLs at rest (ADR-09)
 * @param fingerprintKey key for fingerprinting subscriber addresses
 */
@Validated
@ConfigurationProperties("alerting.security")
public record AlertingSecurityProperties(
        @DefaultValue @NotNull List<@NotBlank @Email String> adminAllowList,
        @DefaultValue("30m") @NotNull @DurationMin(minutes = 1) Duration sessionTimeout,
        String encryptionKey,
        String fingerprintKey) {

    @Override
    public String toString() {
        return "AlertingSecurityProperties[adminAllowList=" + adminAllowList.size() + " entries"
                + ", sessionTimeout=" + sessionTimeout
                + ", encryptionKey=" + Secrets.mask(encryptionKey)
                + ", fingerprintKey=" + Secrets.mask(fingerprintKey) + "]";
    }
}
