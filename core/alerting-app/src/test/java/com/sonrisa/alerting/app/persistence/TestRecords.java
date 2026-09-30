package com.sonrisa.alerting.app.persistence;

import com.sonrisa.alerting.app.persistence.event.Event;
import com.sonrisa.alerting.app.persistence.notification.Notification;
import com.sonrisa.alerting.app.persistence.run.Run;
import com.sonrisa.alerting.app.persistence.run.RunTrigger;
import java.time.Instant;
import java.util.UUID;

/** Builders for valid records in repository tests. */
public final class TestRecords {

    public static final Instant T0 = Instant.parse("2026-09-01T10:00:00.123456Z");

    private TestRecords() {
    }

    public static Run run() {
        return new Run(UUID.randomUUID(), RunTrigger.SCHEDULED, T0);
    }

    public static Event event(String eventKey, Run run) {
        return new Event(UUID.randomUUID(), eventKey, "newsapi", "BBC News", "Title of " + eventKey,
                "Content of " + eventKey, "https://example.org/" + eventKey, T0.minusSeconds(600), T0, run.getId());
    }

    public static Notification notification(Event event, UUID subscriberId, String channelKey) {
        return new Notification(UUID.randomUUID(), event.getId(), subscriberId, event.getRunId(), channelKey, T0);
    }
}
