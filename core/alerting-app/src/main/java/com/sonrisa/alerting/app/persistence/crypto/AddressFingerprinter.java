package com.sonrisa.alerting.app.persistence.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Keyed fingerprint of a subscriber address: HMAC-SHA256 as 64 lower-case hex characters (ADR-09, FR-05). The same
 * function for every subscriber type, so one unique column enforces "one subscription per address" without
 * decrypting anything.
 *
 * <p>The caller passes the address already normalised by its subscriber type (for example an email address in
 * lower case), because only the type knows which differences are insignificant. Thread-safe.
 */
public class AddressFingerprinter {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public AddressFingerprinter(byte[] key) {
        this.key = new SecretKeySpec(key.clone(), ALGORITHM);
    }

    public String fingerprint(String normalizedAddress) {
        Objects.requireNonNull(normalizedAddress, "normalizedAddress");
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(normalizedAddress.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }

    @Override
    public String toString() {
        return "AddressFingerprinter[" + ALGORITHM + "]";
    }
}
