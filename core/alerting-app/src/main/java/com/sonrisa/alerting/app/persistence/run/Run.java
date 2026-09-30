package com.sonrisa.alerting.app.persistence.run;

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

/** One collection and delivery run with its counts (Section 8.1, NFR-16). */
@Entity
@Table(name = "run")
public class Run extends AssignedIdEntity {

    /** Named {@code trigger} in Section 8.1; {@code run_trigger} avoids an SQL keyword. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "run_trigger", nullable = false, updatable = false, length = 16)
    private RunTrigger trigger;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 16)
    private RunStatus status;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "new_events", nullable = false)
    private int newEvents;

    @Column(name = "sent", nullable = false)
    private int sent;

    @Column(name = "failed", nullable = false)
    private int failed;

    @Column(name = "error_summary", length = 2000)
    private String errorSummary;

    /** For JPA only. */
    protected Run() {
    }

    /** A run that has just started. */
    public Run(UUID id, RunTrigger trigger, Instant startedAt) {
        super(id);
        this.trigger = Objects.requireNonNull(trigger, "trigger");
        this.status = RunStatus.RUNNING;
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
    }

    public RunTrigger getTrigger() {
        return trigger;
    }

    public RunStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public int getNewEvents() {
        return newEvents;
    }

    public int getSent() {
        return sent;
    }

    public int getFailed() {
        return failed;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public void recordCounts(int newEvents, int sent, int failed) {
        this.newEvents = newEvents;
        this.sent = sent;
        this.failed = failed;
    }

    /** Ends the run. The error summary must not contain personal data or secrets (NFR-16). */
    public void finish(RunStatus finalStatus, Instant finishedAt, String errorSummary) {
        if (Objects.requireNonNull(finalStatus, "finalStatus") == RunStatus.RUNNING) {
            throw new IllegalArgumentException("a finished run cannot be RUNNING");
        }
        this.status = finalStatus;
        this.finishedAt = Objects.requireNonNull(finishedAt, "finishedAt");
        this.errorSummary = errorSummary;
    }
}
