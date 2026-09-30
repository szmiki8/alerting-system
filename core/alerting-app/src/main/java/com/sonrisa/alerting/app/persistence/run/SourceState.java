package com.sonrisa.alerting.app.persistence.run;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Runtime state of a configured source (Section 8.1): when it last delivered successfully. The sources themselves
 * are defined in configuration (FR-33), so this is the only per-source row.
 */
@Entity
@Table(name = "source_state")
public class SourceState {

    @Id
    @Column(name = "source_key", nullable = false, updatable = false, length = 64)
    private String sourceKey;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    /** For JPA only. */
    protected SourceState() {
    }

    public SourceState(String sourceKey) {
        this.sourceKey = Objects.requireNonNull(sourceKey, "sourceKey");
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public Instant getLastSuccessAt() {
        return lastSuccessAt;
    }

    public void recordSuccess(Instant at) {
        this.lastSuccessAt = Objects.requireNonNull(at, "at");
    }
}
