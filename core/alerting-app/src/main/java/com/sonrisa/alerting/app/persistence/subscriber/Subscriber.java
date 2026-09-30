package com.sonrisa.alerting.app.persistence.subscriber;

import com.sonrisa.alerting.app.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A subscriber of any type (generic model, ADR-11, Section 8.1). The subscriber type plugin gives meaning to
 * {@code displayName} (a name, or the optional label of a webhook) and {@code address} (an email address, a
 * webhook URL, ...). Uniqueness across all types comes from {@code addressFingerprint}.
 *
 * <p>When the subscriber type declares the address secret (a Slack webhook URL), the address is stored encrypted
 * and {@code addressEncrypted} is set (ADR-09, NFR-04). {@link SubscriberAddressListener} encrypts it before the
 * insert and decrypts it after loading, so {@link #getAddress()} always returns the plain address. The flag on the
 * row, never the column content, decides whether a stored value is decrypted. The address never changes after
 * the insert.
 *
 * <p>{@link #toString()} shows only the masked address, never the address or the name (NFR-16).
 */
@Entity
@Table(name = "subscriber")
@EntityListeners(SubscriberAddressListener.class)
public class Subscriber extends AssignedIdEntity {

    @Column(name = "type", nullable = false, updatable = false, length = 32)
    private String type;

    @Column(name = "display_name", length = 200)
    private String displayName;

    /** The address as stored: plain, or encrypted when {@link #addressEncrypted} is set. */
    @Column(name = "address", nullable = false, updatable = false, length = 1024)
    private String storedAddress;

    @Column(name = "address_encrypted", nullable = false, updatable = false)
    private boolean addressEncrypted;

    /** The plain address; set on creation and by {@link SubscriberAddressListener} after loading. */
    @Transient
    private String address;

    @Column(name = "address_fingerprint", nullable = false, updatable = false, length = 64)
    private String addressFingerprint;

    @Column(name = "address_masked", nullable = false, length = 200)
    private String addressMasked;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 16)
    private SubscriberStatus status;

    @Column(name = "subscribed_at", nullable = false, updatable = false)
    private Instant subscribedAt;

    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt;

    /** For JPA only. */
    protected Subscriber() {
    }

    /**
     * A new, active subscriber.
     *
     * @param addressSecret whether the subscriber type declares the address secret, so it is stored encrypted
     */
    public Subscriber(UUID id, String type, String displayName, String address, boolean addressSecret,
            String addressFingerprint, String addressMasked, Instant subscribedAt) {
        super(id);
        this.type = Objects.requireNonNull(type, "type");
        this.displayName = displayName;
        this.address = Objects.requireNonNull(address, "address");
        this.addressEncrypted = addressSecret;
        // A secret address gets its stored form only from the listener; without it the insert fails (NOT NULL).
        this.storedAddress = addressSecret ? null : address;
        this.addressFingerprint = Objects.requireNonNull(addressFingerprint, "addressFingerprint");
        this.addressMasked = Objects.requireNonNull(addressMasked, "addressMasked");
        this.status = SubscriberStatus.ACTIVE;
        this.subscribedAt = Objects.requireNonNull(subscribedAt, "subscribedAt");
        this.statusChangedAt = subscribedAt;
    }

    public String getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** The address in plain form. */
    public String getAddress() {
        return address;
    }

    /** Whether the address is stored encrypted. */
    public boolean isAddressEncrypted() {
        return addressEncrypted;
    }

    public String getAddressFingerprint() {
        return addressFingerprint;
    }

    public String getAddressMasked() {
        return addressMasked;
    }

    public SubscriberStatus getStatus() {
        return status;
    }

    public Instant getSubscribedAt() {
        return subscribedAt;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    /** Activates or deactivates the subscriber; records the time only when the status really changes. */
    public void changeStatus(SubscriberStatus newStatus, Instant changedAt) {
        Objects.requireNonNull(newStatus, "newStatus");
        Objects.requireNonNull(changedAt, "changedAt");
        if (newStatus != status) {
            this.status = newStatus;
            this.statusChangedAt = changedAt;
        }
    }

    String getStoredAddress() {
        return storedAddress;
    }

    /** Called by {@link SubscriberAddressListener} before the insert. */
    void setStoredAddress(String storedAddress) {
        this.storedAddress = storedAddress;
    }

    /** Called by {@link SubscriberAddressListener} after loading. */
    void setPlainAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Subscriber[id=" + getId() + ", type=" + type + ", address=" + addressMasked + ", status=" + status + "]";
    }
}
