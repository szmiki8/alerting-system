package com.sonrisa.alerting.spi.source;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * One item from a source in the standard event format (CON-05, FR-09). The source key is not part of
 * the draft; the application adds it.
 *
 * <p>The constructor rejects drafts that break the format, so a source should drop an item without a
 * title <em>before</em> creating a draft, instead of catching the exception.
 *
 * @param occurredAt when the event happened (for news: the publication time), as an instant; the
 *     application renders it in the configured zone
 * @param title the headline; must not be blank (items without a title are discarded, FR-09)
 * @param content a short text or summary; must not be {@code null}, may be empty when the source has none
 * @param sourceName the human-readable origin shown in notifications, for example the publisher
 *     "BBC News"; must not be blank
 * @param link the original article, if the source provides one; absolute {@code http} or {@code https}
 *     URI
 * @param identity a stable identity of the item within this source (for example the normalised article
 *     URL), if the source has one. The application builds the event key from the source key and this
 *     identity; without it, the application derives one from the link or from title plus
 *     {@code occurredAt}. Must not be blank when present.
 */
public record EventDraft(
        Instant occurredAt,
        String title,
        String content,
        String sourceName,
        @Nullable URI link,
        @Nullable String identity) {

    public EventDraft {
        Objects.requireNonNull(occurredAt, "occurredAt");
        requireText(title, "title");
        Objects.requireNonNull(content, "content");
        requireText(sourceName, "sourceName");
        if (link != null) {
            String scheme = link.getScheme();
            if (!link.isAbsolute() || scheme == null
                    || !(scheme.toLowerCase(Locale.ROOT).equals("http")
                    || scheme.toLowerCase(Locale.ROOT).equals("https"))) {
                throw new IllegalArgumentException("link must be an absolute http or https URI");
            }
        }
        if (identity != null && identity.isBlank()) {
            throw new IllegalArgumentException("identity must not be blank when present");
        }
    }

    /** A draft without link and without its own identity. */
    public EventDraft(Instant occurredAt, String title, String content, String sourceName) {
        this(occurredAt, title, content, sourceName, null, null);
    }

    private static void requireText(@Nullable String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
