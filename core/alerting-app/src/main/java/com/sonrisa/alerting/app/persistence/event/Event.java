package com.sonrisa.alerting.app.persistence.event;

import com.sonrisa.alerting.app.persistence.AssignedIdEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A collected event in the standard format (CON-05, FR-09, Section 8.1). Everything except {@code link} is
 * mandatory. {@code eventKey} is unique, so an event is stored only once (FR-10). Events never change.
 */
@Entity
@Table(name = "event")
public class Event extends AssignedIdEntity {

    @Column(name = "event_key", nullable = false, updatable = false, length = 255)
    private String eventKey;

    /** Key of the source plugin, for example {@code newsapi}. */
    @Column(name = "source_key", nullable = false, updatable = false, length = 64)
    private String sourceKey;

    /** Human-readable source shown in notifications, for example "BBC News" (FR-09, FR-17). */
    @Column(name = "source_name", nullable = false, updatable = false, length = 200)
    private String sourceName;

    @Column(name = "title", nullable = false, updatable = false, length = 500)
    private String title;

    @Column(name = "content", nullable = false, updatable = false, length = 4000)
    private String content;

    @Column(name = "link", updatable = false, length = 2048)
    private String link;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "collected_at", nullable = false, updatable = false)
    private Instant collectedAt;

    @Column(name = "run_id", nullable = false, updatable = false)
    private UUID runId;

    /** For JPA only. */
    protected Event() {
    }

    public Event(UUID id, String eventKey, String sourceKey, String sourceName, String title, String content,
            String link, Instant occurredAt, Instant collectedAt, UUID runId) {
        super(id);
        this.eventKey = Objects.requireNonNull(eventKey, "eventKey");
        this.sourceKey = Objects.requireNonNull(sourceKey, "sourceKey");
        this.sourceName = Objects.requireNonNull(sourceName, "sourceName");
        this.title = Objects.requireNonNull(title, "title");
        this.content = Objects.requireNonNull(content, "content");
        this.link = link;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.collectedAt = Objects.requireNonNull(collectedAt, "collectedAt");
        this.runId = Objects.requireNonNull(runId, "runId");
    }

    public String getEventKey() {
        return eventKey;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    /** The link to the original, or {@code null}. */
    public String getLink() {
        return link;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public UUID getRunId() {
        return runId;
    }
}
