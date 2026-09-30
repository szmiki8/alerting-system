package com.sonrisa.alerting.app.persistence.crypto;

import com.sonrisa.alerting.app.config.AlertingSecurityProperties;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the encryptor and the fingerprinter from the security group (BE-09, ADR-09). Keys come from the
 * environment or the secret store and have already been validated by {@link AlertingSecurityProperties}.
 *
 * <p>Without keys, the deployed profiles have already refused to start ({@code keys-required}). In {@code local}
 * and {@code test} a random throw-away key is generated instead, with a warning: data encrypted with it cannot be
 * read after a restart, which is harmless for the in-memory database.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AlertingSecurityProperties.class)
public class CryptoConfiguration {

    static final String GENERATED_KEY_ID = "generated";

    private static final Logger log = LoggerFactory.getLogger(CryptoConfiguration.class);

    @Bean
    AttributeEncryptor attributeEncryptor(AlertingSecurityProperties security) {
        SecureRandom random = new SecureRandom();
        Map<String, SecretKey> keys = new LinkedHashMap<>();
        security.encryptionOldKeys().forEach(key -> putKey(keys, key));
        String activeKeyId;
        if (security.hasEncryptionKey()) {
            activeKeyId = putKey(keys, security.encryptionKey());
        } else {
            log.warn("No encryption key configured (ALERTING_SECURITY_ENCRYPTION_KEY): using a generated throw-away"
                    + " key. Encrypted subscriber addresses cannot be read after a restart.");
            activeKeyId = GENERATED_KEY_ID;
            keys.put(activeKeyId, new SecretKeySpec(randomBytes(random, AlertingSecurityProperties.ENCRYPTION_KEY_BYTES),
                    "AES"));
        }
        return new AttributeEncryptor(activeKeyId, keys, random);
    }

    @Bean
    AddressFingerprinter addressFingerprinter(AlertingSecurityProperties security) {
        if (security.hasFingerprintKey()) {
            return new AddressFingerprinter(Base64.getDecoder().decode(security.fingerprintKey().strip()));
        }
        log.warn("No fingerprint key configured (ALERTING_SECURITY_FINGERPRINT_KEY): using a generated throw-away"
                + " key. Duplicate checks do not work across a restart.");
        return new AddressFingerprinter(
                randomBytes(new SecureRandom(), AlertingSecurityProperties.MIN_FINGERPRINT_KEY_BYTES));
    }

    /** Adds a {@code <key-id>:<base64>} key and returns its id. */
    private static String putKey(Map<String, SecretKey> keys, String value) {
        String trimmed = value.strip();
        int colon = trimmed.indexOf(':');
        String keyId = trimmed.substring(0, colon);
        keys.put(keyId, new SecretKeySpec(Base64.getDecoder().decode(trimmed.substring(colon + 1)), "AES"));
        return keyId;
    }

    private static byte[] randomBytes(SecureRandom random, int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }
}
