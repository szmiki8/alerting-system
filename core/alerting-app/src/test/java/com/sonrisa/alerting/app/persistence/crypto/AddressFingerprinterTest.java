package com.sonrisa.alerting.app.persistence.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class AddressFingerprinterTest {

    static byte[] key(int seed) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) seed);
        return bytes;
    }

    @Test
    void isStableHexOfHmacSha256() {
        AddressFingerprinter fingerprinter = new AddressFingerprinter(key(1));

        String fingerprint = fingerprinter.fingerprint("ada@example.org");

        assertThat(fingerprint).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(new AddressFingerprinter(key(1)).fingerprint("ada@example.org")).isEqualTo(fingerprint);
    }

    @Test
    void sameFunctionForEveryType() {
        AddressFingerprinter fingerprinter = new AddressFingerprinter(key(1));

        assertThat(fingerprinter.fingerprint("https://hooks.slack.com/services/T0/B0/X"))
                .hasSize(64)
                .isNotEqualTo(fingerprinter.fingerprint("ada@example.org"));
    }

    @Test
    void dependsOnTheKey() {
        assertThat(new AddressFingerprinter(key(1)).fingerprint("ada@example.org"))
                .isNotEqualTo(new AddressFingerprinter(key(2)).fingerprint("ada@example.org"));
    }

    @Test
    void doesNotNormalise() {
        // Normalisation belongs to the subscriber type; the fingerprinter hashes exactly what it gets.
        AddressFingerprinter fingerprinter = new AddressFingerprinter(key(1));

        assertThat(fingerprinter.fingerprint("Ada@Example.org")).isNotEqualTo(fingerprinter.fingerprint("ada@example.org"));
    }
}
