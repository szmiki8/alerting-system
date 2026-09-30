package com.sonrisa.alerting.spi.source;

import java.util.List;

/**
 * Fetches items from one external service and returns them as {@link EventDraft event drafts} in the
 * standard event format (CON-05, FR-09). Architecture Section 7.1.
 *
 * <p><b>Responsible for:</b>
 * <ul>
 *   <li>a unique, stable {@link #key() key};</li>
 *   <li>fetching with its own filters, read from its configuration group {@code alerting.sources.<key>.*};</li>
 *   <li>returning at most {@link FetchContext#maxEvents()} drafts;</li>
 *   <li>mapping each item to an {@link EventDraft} and dropping items without a title;</li>
 *   <li>a timeout on every outbound call, so that one call never blocks the run;</li>
 *   <li>classifying failures as transient or permanent by throwing an {@link EventSourceException}.</li>
 * </ul>
 *
 * <p><b>Not responsible for:</b> duplicate detection (the application computes the event key and stores
 * only new events), storage, retries across calls (the application retries transient failures), scheduling,
 * and remembering the time of the last successful fetch (the application passes it in the context).
 * A source must therefore not filter out items only because it returned them before.
 *
 * <p>Implementations must be thread-safe and should hold no state between calls.
 */
public interface EventSource {

    /**
     * The source key, for example {@code newsapi}. Lower-case letters, digits and hyphens, starting with a
     * letter. It is stored with every event and names the configuration group, so it must never change.
     *
     * @return the key; the same value on every call
     */
    String key();

    /**
     * Fetches the newest items and maps them to event drafts.
     *
     * <p>The result may contain items the application already stored; it deduplicates them. The order of
     * the list does not matter.
     *
     * @param context the last successful fetch as a hint (sources that support it can use it to fill
     *     gaps after downtime, NFR-09) and the maximum number of drafts to return
     * @return at most {@code context.maxEvents()} drafts, possibly empty; never {@code null}
     * @throws EventSourceException when the fetch failed. {@link EventSourceException.Kind#TRANSIENT}
     *     failures (time-outs, HTTP 5xx, rate limits) are retried by the application;
     *     {@link EventSourceException.Kind#PERMANENT} failures (invalid credentials, invalid configuration)
     *     are not retried in this run. Any other runtime exception is treated like a transient failure.
     */
    List<EventDraft> fetchNewItems(FetchContext context) throws EventSourceException;
}
