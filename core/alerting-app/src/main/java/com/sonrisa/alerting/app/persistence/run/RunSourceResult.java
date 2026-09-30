package com.sonrisa.alerting.app.persistence.run;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Result of one source within a run (Section 8.1, NFR-16). Deleted together with its run. */
@Entity
@Table(name = "run_source_result")
public class RunSourceResult {

    @EmbeddedId
    private RunSourceResultId id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 16)
    private RunSourceResultStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "new_events", nullable = false)
    private int newEvents;

    @Column(name = "error_summary", length = 2000)
    private String errorSummary;

    /** For JPA only. */
    protected RunSourceResult() {
    }

    public RunSourceResult(RunSourceResultId id, RunSourceResultStatus status, int attempts, int newEvents,
            String errorSummary) {
        this.id = Objects.requireNonNull(id, "id");
        this.status = Objects.requireNonNull(status, "status");
        this.attempts = attempts;
        this.newEvents = newEvents;
        this.errorSummary = errorSummary;
    }

    public RunSourceResultId getId() {
        return id;
    }

    public RunSourceResultStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getNewEvents() {
        return newEvents;
    }

    public String getErrorSummary() {
        return errorSummary;
    }
}
