package com.sonrisa.alerting.spi.source;

import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * What the application tells a source for one fetch.
 *
 * @param lastSuccessAt the start of the last successful fetch from this source, or {@code null} if there
 *     was none (first run, or the state was lost with the in-memory database). Only a hint: a source that
 *     cannot filter by time ignores it.
 * @param maxEvents the maximum number of drafts the source may return in this run; at least 1
 *     (configured per source, default 10, AQ-03)
 */
public record FetchContext(@Nullable Instant lastSuccessAt, int maxEvents) {

    public FetchContext {
        if (maxEvents < 1) {
            throw new IllegalArgumentException("maxEvents must be at least 1, was " + maxEvents);
        }
    }

    /** The last successful fetch, if there was one. */
    public Optional<Instant> lastSuccess() {
        return Optional.ofNullable(lastSuccessAt);
    }
}
