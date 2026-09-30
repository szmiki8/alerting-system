package com.sonrisa.alerting.app.persistence.subscriber;

import com.sonrisa.alerting.app.persistence.crypto.AttributeEncryptor;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;

/**
 * Encrypts secret subscriber addresses before the insert and decrypts them after loading (BE-09, ADR-09). Whether
 * to encrypt comes from the row's {@code address_encrypted} flag, set from the subscriber type, never from the
 * column content. Hibernate creates this listener through Spring, so the encryptor is injected.
 *
 * <p>Addresses never change after the insert (the column is not updatable), so there is no {@code @PreUpdate}.
 */
public class SubscriberAddressListener {

    private final AttributeEncryptor encryptor;

    public SubscriberAddressListener(AttributeEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @PrePersist
    void protect(Subscriber subscriber) {
        if (subscriber.isAddressEncrypted()) {
            subscriber.setStoredAddress(encryptor.encrypt(subscriber.getAddress()));
        }
    }

    @PostLoad
    void reveal(Subscriber subscriber) {
        String stored = subscriber.getStoredAddress();
        subscriber.setPlainAddress(subscriber.isAddressEncrypted() ? encryptor.decrypt(stored) : stored);
    }
}
