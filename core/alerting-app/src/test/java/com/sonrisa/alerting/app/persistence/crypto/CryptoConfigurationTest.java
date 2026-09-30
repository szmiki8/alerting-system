package com.sonrisa.alerting.app.persistence.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Keys from configuration, generated throw-away keys and the start-up checks (BE-09). */
@ExtendWith(OutputCaptureExtension.class)
class CryptoConfigurationTest {

    static final byte[] KEY_BYTES_1 = bytes(1);
    static final byte[] KEY_BYTES_2 = bytes(2);
    static final String KEY_1 = Base64.getEncoder().encodeToString(KEY_BYTES_1);
    static final String KEY_2 = Base64.getEncoder().encodeToString(KEY_BYTES_2);

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CryptoConfiguration.class);

    static byte[] bytes(int seed) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) seed);
        return bytes;
    }

    @Test
    void usesTheConfiguredKeys() {
        runner.withPropertyValues(
                        "alerting.security.encryption-key=k2:" + KEY_2,
                        "alerting.security.fingerprint-key=" + KEY_1)
                .run(context -> {
                    String stored = context.getBean(AttributeEncryptor.class).encrypt("secret");
                    var sameKey = new AttributeEncryptor("k2", Map.of("k2", new SecretKeySpec(KEY_BYTES_2, "AES")),
                            new SecureRandom());
                    assertThat(stored).startsWith("k2:");
                    assertThat(sameKey.decrypt(stored)).isEqualTo("secret");
                    assertThat(context.getBean(AddressFingerprinter.class).fingerprint("a@example.org"))
                            .isEqualTo(new AddressFingerprinter(KEY_BYTES_1).fingerprint("a@example.org"));
                });
    }

    @Test
    void readsDataWrittenWithAnOldKey() {
        String writtenWithOldKey = new AttributeEncryptor("k1", Map.of("k1", new SecretKeySpec(KEY_BYTES_1, "AES")),
                new SecureRandom()).encrypt("secret");

        runner.withPropertyValues(
                        "alerting.security.encryption-key=k2:" + KEY_2,
                        "alerting.security.encryption-old-keys=k1:" + KEY_1,
                        "alerting.security.fingerprint-key=" + KEY_1)
                .run(context -> {
                    AttributeEncryptor encryptor = context.getBean(AttributeEncryptor.class);
                    assertThat(encryptor.decrypt(writtenWithOldKey)).isEqualTo("secret");
                    assertThat(encryptor.encrypt("secret")).startsWith("k2:");
                });
    }

    @Test
    void generatesThrowAwayKeysWithAWarningWhenNotRequired(CapturedOutput output) {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            AttributeEncryptor encryptor = context.getBean(AttributeEncryptor.class);
            assertThat(encryptor.decrypt(encryptor.encrypt("secret"))).isEqualTo("secret");
            assertThat(context.getBean(AddressFingerprinter.class).fingerprint("a@example.org")).hasSize(64);
        });
        assertThat(output.getOut() + output.getErr())
                .contains("No encryption key configured (ALERTING_SECURITY_ENCRYPTION_KEY)")
                .contains("No fingerprint key configured (ALERTING_SECURITY_FINGERPRINT_KEY)");
    }

    @Test
    void refusesToStartWithoutKeysWhenRequired() {
        runner.withPropertyValues("alerting.security.keys-required=true",
                        "alerting.security.fingerprint-key=" + KEY_1)
                .run(context -> assertThat(context).getFailure()
                        .rootCause()
                        .hasMessageContaining("alerting.security")
                        .hasMessageContaining("must be set through ALERTING_SECURITY_ENCRYPTION_KEY and"
                                + " ALERTING_SECURITY_FINGERPRINT_KEY in this profile"));
    }

    @Test
    void startsWithKeysWhenRequired() {
        runner.withPropertyValues("alerting.security.keys-required=true",
                        "alerting.security.encryption-key=k1:" + KEY_1,
                        "alerting.security.fingerprint-key=" + KEY_2)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void rejectsMalformedKeysWithoutShowingThem() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        for (String encryptionKey : new String[] {KEY_1, "k1:" + shortKey, "bad id:" + KEY_1, "k1:not-base64!"}) {
            runner.withPropertyValues("alerting.security.encryption-key=" + encryptionKey)
                    .run(context -> assertThat(context).getFailure()
                            .rootCause()
                            .hasMessageContaining("<key-id>:<base64 of a 32-byte key>")
                            .hasMessageNotContaining(KEY_1)
                            .hasMessageNotContaining(shortKey));
        }
    }

    @Test
    void rejectsDuplicateKeyIds() {
        runner.withPropertyValues(
                        "alerting.security.encryption-key=k1:" + KEY_1,
                        "alerting.security.encryption-old-keys=k1:" + KEY_2)
                .run(context -> assertThat(context).getFailure().rootCause().hasMessageContaining("unique key ids"));
    }

    @Test
    void rejectsShortFingerprintKey() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        runner.withPropertyValues("alerting.security.fingerprint-key=" + shortKey)
                .run(context -> assertThat(context).getFailure()
                        .rootCause()
                        .hasMessageContaining("must be base64 of at least 32 bytes")
                        .hasMessageNotContaining(shortKey));
    }
}
