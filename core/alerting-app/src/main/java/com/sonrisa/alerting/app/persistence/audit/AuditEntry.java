package com.sonrisa.alerting.app.persistence.audit;

import com.sonrisa.alerting.app.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One admin action (FR-30, Section 8.2): who did what to which type of subscriber, and when. It holds no personal
 * data of the affected subscriber, so it can outlive the deletion. Entries are never changed.
 */
@Entity
@Table(name = "audit_entry")
public class AuditEntry extends AssignedIdEntity {

    @Column(name = "admin_email", nullable = false, updatable = false, length = 320)
    private String adminEmail;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "action", nullable = false, updatable = false, length = 32)
    private AuditAction action;

    @Column(name = "subscriber_type", nullable = false, updatable = false, length = 32)
    private String subscriberType;

    /** Named {@code at} in Section 8.1; {@code performed_at} avoids an SQL keyword. */
    @Column(name = "performed_at", nullable = false, updatable = false)
    private Instant performedAt;

    /** For JPA only. */
    protected AuditEntry() {
    }

    public AuditEntry(UUID id, String adminEmail, AuditAction action, String subscriberType, Instant performedAt) {
        super(id);
        this.adminEmail = Objects.requireNonNull(adminEmail, "adminEmail");
        this.action = Objects.requireNonNull(action, "action");
        this.subscriberType = Objects.requireNonNull(subscriberType, "subscriberType");
        this.performedAt = Objects.requireNonNull(performedAt, "performedAt");
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getSubscriberType() {
        return subscriberType;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }
}
