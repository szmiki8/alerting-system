package com.sonrisa.alerting.app.persistence.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class AttributeEncryptorTest {

    static final String WEBHOOK = "https://hooks.slack.com/services/T000/B000/XXXXXXXXXXXXXXXXXXXXXXXX";

    static final SecretKey KEY_1 = key(1);
    static final SecretKey KEY_2 = key(2);

    static SecretKey key(int seed) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) seed);
        return new SecretKeySpec(bytes, "AES");
    }

    static AttributeEncryptor encryptor(String activeKeyId, Map<String, SecretKey> keys) {
        return new AttributeEncryptor(activeKeyId, keys, new SecureRandom());
    }

    @Test
    void roundTrip() {
        AttributeEncryptor encryptor = encryptor("k1", Map.of("k1", KEY_1));

        String stored = encryptor.encrypt(WEBHOOK);

        assertThat(stored).startsWith("k1:").doesNotContain("hooks.slack.com").doesNotContain("XXXX");
        assertThat(encryptor.decrypt(stored)).isEqualTo(WEBHOOK);
    }

    @Test
    void sameValueEncryptsDifferentlyEachTime() {
        AttributeEncryptor encryptor = encryptor("k1", Map.of("k1", KEY_1));

        assertThat(encryptor.encrypt(WEBHOOK)).isNotEqualTo(encryptor.encrypt(WEBHOOK));
    }

    @Test
    void detectsTamperedCiphertext() {
        AttributeEncryptor encryptor = encryptor("k1", Map.of("k1", KEY_1));
        String stored = encryptor.encrypt(WEBHOOK);
        byte[] payload = Base64.getDecoder().decode(stored.substring(3));
        payload[payload.length / 2] ^= 1;
        String tampered = "k1:" + Base64.getEncoder().encodeToString(payload);

        assertThatThrownBy(() -> encryptor.decrypt(tampered))
                .isInstanceOf(AttributeEncryptionException.class)
                .hasMessageContaining("failed authentication");
    }

    @Test
    void detectsChangedKeyId() {
        // Both ids are configured with the same key: only the authenticated key id differs.
        AttributeEncryptor encryptor = encryptor("k1", Map.of("k1", KEY_1, "k2", KEY_1));
        String stored = encryptor.encrypt(WEBHOOK);

        assertThatThrownBy(() -> encryptor.decrypt("k2" + stored.substring(2)))
                .isInstanceOf(AttributeEncryptionException.class)
                .hasMessageContaining("failed authentication");
    }

    @Test
    void wrongKeyCannotDecrypt() {
        String stored = encryptor("k1", Map.of("k1", KEY_1)).encrypt(WEBHOOK);
        AttributeEncryptor withWrongKey = encryptor("k1", Map.of("k1", KEY_2));

        assertThatThrownBy(() -> withWrongKey.decrypt(stored))
                .isInstanceOf(AttributeEncryptionException.class)
                .hasMessageContaining("wrong key or tampered value")
                .hasMessageNotContaining(stored.substring(3));
    }

    @Test
    void unknownKeyIdIsReported() {
        String stored = encryptor("old", Map.of("old", KEY_1)).encrypt(WEBHOOK);
        AttributeEncryptor withoutOldKey = encryptor("new", Map.of("new", KEY_2));

        assertThatThrownBy(() -> withoutOldKey.decrypt(stored))
                .isInstanceOf(AttributeEncryptionException.class)
                .hasMessage("No key is configured for key id 'old'");
    }

    @Test
    void keyRotationReadsOldDataAndWritesWithTheNewKey() {
        String writtenWithOldKey = encryptor("2025", Map.of("2025", KEY_1)).encrypt(WEBHOOK);
        AttributeEncryptor rotated = encryptor("2026", Map.of("2026", KEY_2, "2025", KEY_1));

        assertThat(rotated.decrypt(writtenWithOldKey)).isEqualTo(WEBHOOK);
        assertThat(rotated.encrypt(WEBHOOK)).startsWith("2026:");
        assertThat(rotated.decrypt(rotated.encrypt(WEBHOOK))).isEqualTo(WEBHOOK);
    }

    @Test
    void rejectsValuesThatAreNotEncrypted() {
        AttributeEncryptor encryptor = encryptor("k1", Map.of("k1", KEY_1));

        assertThatThrownBy(() -> encryptor.decrypt("plain value")).isInstanceOf(AttributeEncryptionException.class)
                .hasMessageNotContaining("plain value");
        assertThatThrownBy(() -> encryptor.decrypt("k1:not base64!")).isInstanceOf(AttributeEncryptionException.class)
                .hasMessageNotContaining("not base64");
        assertThatThrownBy(() -> encryptor.decrypt("k1:AAAA")).isInstanceOf(AttributeEncryptionException.class);
    }

    @Test
    void activeKeyMustBeConfigured() {
        assertThatThrownBy(() -> encryptor("missing", Map.of("k1", KEY_1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringShowsNoKeys() {
        assertThat(encryptor("k1", Map.of("k1", KEY_1)))
                .hasToString("AttributeEncryptor[activeKeyId=k1, keys=1]");
    }
}
