package com.sonrisa.alerting.app.persistence.crypto;

/**
 * A value could not be encrypted or decrypted. The message never contains the plaintext, the ciphertext or a key;
 * at most the key id, which is not secret.
 */
public class AttributeEncryptionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AttributeEncryptionException(String message) {
        super(message);
    }

    public AttributeEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
