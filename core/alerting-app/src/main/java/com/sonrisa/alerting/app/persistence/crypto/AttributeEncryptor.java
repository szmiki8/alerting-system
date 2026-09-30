package com.sonrisa.alerting.app.persistence.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Authenticated encryption of single attribute values with AES-256-GCM (ADR-09, NFR-04).
 *
 * <p>Stored form: {@code <key-id>:<base64(iv | ciphertext | tag)>}. A fresh random 96-bit IV per value; the key id
 * is authenticated as associated data, so changing it is detected like any other tampering. New values use the
 * active key; values written with any configured key can be read, which allows key rotation: configure a new active
 * key and keep the old one as a read-only key.
 *
 * <p>Thread-safe. Exceptions and {@link #toString()} never reveal plaintext, ciphertext or keys.
 */
public class AttributeEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final char SEPARATOR = ':';

    private final String activeKeyId;
    private final Map<String, SecretKey> keys;
    private final SecureRandom random;

    /**
     * @param activeKeyId id of the key for new values; must be one of {@code keys}
     * @param keys all usable AES keys by id (the active one and older ones)
     * @param random source of IVs
     */
    public AttributeEncryptor(String activeKeyId, Map<String, SecretKey> keys, SecureRandom random) {
        this.activeKeyId = Objects.requireNonNull(activeKeyId, "activeKeyId");
        this.keys = Map.copyOf(keys);
        this.random = Objects.requireNonNull(random, "random");
        if (!this.keys.containsKey(activeKeyId)) {
            throw new IllegalArgumentException("active key id '" + activeKeyId + "' has no key");
        }
    }

    public String activeKeyId() {
        return activeKeyId;
    }

    public String encrypt(String plaintext) {
        Objects.requireNonNull(plaintext, "plaintext");
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, keys.get(activeKeyId), iv, activeKeyId);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(IV_BYTES + ciphertext.length).put(iv).put(ciphertext).array();
            return activeKeyId + SEPARATOR + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            throw new AttributeEncryptionException("Encryption with key id '" + activeKeyId + "' failed", e);
        }
    }

    public String decrypt(String stored) {
        Objects.requireNonNull(stored, "stored");
        int separator = stored.indexOf(SEPARATOR);
        if (separator <= 0) {
            throw new AttributeEncryptionException("Stored value is not in the encrypted format");
        }
        String keyId = stored.substring(0, separator);
        SecretKey key = keys.get(keyId);
        if (key == null) {
            throw new AttributeEncryptionException("No key is configured for key id '" + keyId + "'");
        }
        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(stored.substring(separator + 1));
        } catch (IllegalArgumentException e) {
            throw new AttributeEncryptionException("Stored value is not in the encrypted format");
        }
        if (payload.length < IV_BYTES + TAG_BITS / 8) {
            throw new AttributeEncryptionException("Stored value is too short to be encrypted");
        }
        try {
            Cipher cipher = cipher(Cipher.DECRYPT_MODE, key, Arrays.copyOf(payload, IV_BYTES), keyId);
            byte[] plaintext = cipher.doFinal(payload, IV_BYTES, payload.length - IV_BYTES);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            throw new AttributeEncryptionException(
                    "Stored value failed authentication with key id '" + keyId + "' (wrong key or tampered value)");
        } catch (GeneralSecurityException e) {
            throw new AttributeEncryptionException("Decryption with key id '" + keyId + "' failed", e);
        }
    }

    private static Cipher cipher(int mode, SecretKey key, byte[] iv, String keyId) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(keyId.getBytes(StandardCharsets.UTF_8));
        return cipher;
    }

    @Override
    public String toString() {
        return "AttributeEncryptor[activeKeyId=" + activeKeyId + ", keys=" + keys.size() + "]";
    }
}
