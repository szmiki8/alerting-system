package com.sonrisa.alerting.spi.channel;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Channel-neutral input for rendering one notification: the stored event plus the configured zone
 * (FR-17, CON-10). Rendering (layout, escaping, length limits) is the channel's job.
 *
 * @param title the event title; not blank
 * @param content the short content or summary; may be empty
 * @param sourceName the human-readable source, for example "BBC News"; not blank
 * @param occurredAt when the event happened, as an instant
 * @param link the original article, if known
 * @param zone the zone to show times in (the configured zone, default Europe/Budapest)
 */
public record NotificationContent(
        String title,
        String content,
        String sourceName,
        Instant occurredAt,
        @Nullable URI link,
        ZoneId zone) {

    public NotificationContent {
        requireText(title, "title");
        Objects.requireNonNull(content, "content");
        requireText(sourceName, "sourceName");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(zone, "zone");
    }

    /** {@link #occurredAt()} in the configured {@link #zone()}, ready for formatting. */
    public ZonedDateTime occurredAtInZone() {
        return occurredAt.atZone(zone);
    }

    private static void requireText(@Nullable String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
