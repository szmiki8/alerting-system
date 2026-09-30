package com.sonrisa.alerting.app.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Persistable;

/**
 * Base class for entities with a UUID key generated in the application (Section 8.1). Because the id is known
 * before the insert, {@link Persistable#isNew()} tells Spring Data that a fresh object is new, so
 * {@code save} inserts directly (no select first) and Hibernate can batch the inserts (ADR-06).
 *
 * <p>Equality is by id and entity class; the id never changes.
 */
@MappedSuperclass
public abstract class AssignedIdEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Transient
    private boolean isNew = true;

    /** For JPA only. */
    protected AssignedIdEntity() {
    }

    protected AssignedIdEntity(UUID id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        // Hibernate.getClass unwraps lazy proxies.
        return other instanceof AssignedIdEntity entity && Hibernate.getClass(this) == Hibernate.getClass(entity)
                && id.equals(entity.getId());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
