package com.sonrisa.alerting.app.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Security group (Section 13.1, 13.5). Named with a prefix to avoid confusion with Spring Boot's own
 * {@code SecurityProperties}. The Google client registration stays in Spring's
 * {@code spring.security.oauth2.client.*} keys (BE-20).
 *
 * <p>The keys are secrets: set them only through the environment ({@code ALERTING_SECURITY_ENCRYPTION_KEY},
 * {@code ALERTING_SECURITY_ENCRYPTION_OLD_KEYS}, {@code ALERTING_SECURITY_FINGERPRINT_KEY}) or the secret store.
 * The deployed profiles ({@code demo}, {@code postgres}, {@code aws}) set {@code keys-required}, so they refuse to
 * start without them; {@code local} and {@code test} then use generated throw-away keys (BE-09). Validation
 * messages and {@link #toString()} never show a key.
 *
 * @param adminAllowList email addresses allowed into the admin area (FR-24), compared case-insensitively
 * @param sessionTimeout admin session inactivity timeout (NFR-02), default 30 minutes
 * @param encryptionKey active key for encrypting secret subscriber addresses at rest (ADR-09), in the form
 *     {@code <key-id>:<base64 of 32 bytes>}; new values are encrypted with it
 * @param encryptionOldKeys earlier encryption keys in the same form, only for reading data written with them
 *     (key rotation)
 * @param fingerprintKey HMAC key for fingerprinting subscriber addresses: base64 of at least 32 bytes
 * @param keysRequired whether start-up fails when the encryption or fingerprint key is missing, default
 *     {@code false}
 */
@Validated
@ConfigurationProperties("alerting.security")
public record AlertingSecurityProperties(
        @DefaultValue @NotNull List<@NotBlank @Email String> adminAllowList,
        @DefaultValue("30m") @NotNull @DurationMin(minutes = 1) Duration sessionTimeout,
        String encryptionKey,
        @DefaultValue List<String> encryptionOldKeys,
        String fingerprintKey,
        @DefaultValue("false") boolean keysRequired) {

    /** Key id, colon, base64 key. The key id is stored with every encrypted value. */
    private static final Pattern ENCRYPTION_KEY = Pattern.compile("[A-Za-z0-9_-]{1,32}:[A-Za-z0-9+/=]+");

    /** AES-256. */
    public static final int ENCRYPTION_KEY_BYTES = 32;

    /** At least the output size of HMAC-SHA256. */
    public static final int MIN_FINGERPRINT_KEY_BYTES = 32;

    public boolean hasEncryptionKey() {
        return encryptionKey != null && !encryptionKey.isBlank();
    }

    public boolean hasFingerprintKey() {
        return fingerprintKey != null && !fingerprintKey.isBlank();
    }

    /**
     * Validation rule: a profile that requires the keys must have both. The failure report names this rule, never
     * a key value.
     */
    @AssertTrue(message = "must be set through ALERTING_SECURITY_ENCRYPTION_KEY and ALERTING_SECURITY_FINGERPRINT_KEY"
            + " in this profile")
    public boolean isKeysPresentWhenRequired() {
        return !keysRequired || (hasEncryptionKey() && hasFingerprintKey());
    }

    /** Validation rule: every encryption key has the form {@code <key-id>:<base64 of 32 bytes>}, key ids unique. */
    @AssertTrue(message = "must each have the form <key-id>:<base64 of a 32-byte key> with unique key ids"
            + " (ALERTING_SECURITY_ENCRYPTION_KEY, ALERTING_SECURITY_ENCRYPTION_OLD_KEYS)")
    public boolean isEncryptionKeysWellFormed() {
        List<String> keys = new ArrayList<>(encryptionOldKeys);
        if (hasEncryptionKey()) {
            keys.add(encryptionKey);
        }
        Set<String> keyIds = new HashSet<>();
        for (String key : keys) {
            String value = key == null ? "" : key.strip();
            if (!ENCRYPTION_KEY.matcher(value).matches()) {
                return false;
            }
            int colon = value.indexOf(':');
            if (!keyIds.add(value.substring(0, colon))
                    || decodedLength(value.substring(colon + 1)) != ENCRYPTION_KEY_BYTES) {
                return false;
            }
        }
        return true;
    }

    /** Validation rule: the fingerprint key is base64 of at least 32 bytes. */
    @AssertTrue(message = "must be base64 of at least 32 bytes (ALERTING_SECURITY_FINGERPRINT_KEY)")
    public boolean isFingerprintKeyWellFormed() {
        return !hasFingerprintKey() || decodedLength(fingerprintKey.strip()) >= MIN_FINGERPRINT_KEY_BYTES;
    }

    private static int decodedLength(String base64) {
        try {
            return Base64.getDecoder().decode(base64).length;
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    @Override
    public String toString() {
        return "AlertingSecurityProperties[adminAllowList=" + adminAllowList.size() + " entries"
                + ", sessionTimeout=" + sessionTimeout
                + ", encryptionKey=" + Secrets.mask(encryptionKey)
                + ", encryptionOldKeys=" + encryptionOldKeys.size() + " entries"
                + ", fingerprintKey=" + Secrets.mask(fingerprintKey)
                + ", keysRequired=" + keysRequired + "]";
    }
}
