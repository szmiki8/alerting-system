package com.sonrisa.alerting.spi.channel;

/**
 * Renders and delivers one notification (one event to one recipient) through one medium, for example
 * email, Slack or the log. Architecture Section 7.3.
 *
 * <p><b>Responsible for:</b> a unique {@link #key() key}; {@link #render rendering} the message with the
 * event's title, date and time in the configured zone, source, summary and link (FR-17);
 * {@link #send sending} it with a timeout; classifying the {@link DeliveryResult result} as delivered,
 * transient failure or permanent failure (FR-21); declaring its {@link #pacingLimits() pacing limits}.
 * Settings (sender identity, rate limits, credentials by reference) live under
 * {@code alerting.channels.<key>.*}.
 *
 * <p><b>Not responsible for:</b> choosing recipients, retries and back-off, enforcing the pacing limits,
 * duplicate protection (FR-20), marking subscribers inactive, and storing notification state. The
 * application's delivery service does these. A channel sends exactly once per {@code send} call.
 *
 * <p>Implementations must be thread-safe: the delivery service calls {@code send} from several worker
 * threads at once. They must not log the recipient's address when it is a secret (NFR-04).
 *
 * @param <M> the channel's own rendered message type (for example subject and body for email, a JSON
 *     payload for Slack). The application does not look inside it; it only passes the result of
 *     {@code render} to {@code send}, usually through {@link #deliver}.
 */
public interface NotificationChannel<M> {

    /**
     * The channel key, for example {@code email}, {@code slack} or {@code log}. Lower-case letters,
     * digits and hyphens, starting with a letter. Stored with every notification and referenced by
     * {@link com.sonrisa.alerting.spi.subscriber.SubscriberType#channelKey()}, so it must never change.
     */
    String key();

    /**
     * Renders the message for one recipient. Must not do I/O. The same content may be rendered again for a
     * retry, so the result must depend only on the arguments.
     *
     * @param content the channel-neutral event data and the zone to show times in
     * @param recipient the recipient, for a salutation; the address is only needed by {@code send}
     * @return the rendered message
     */
    M render(NotificationContent content, Recipient recipient);

    /**
     * Sends one rendered message to one recipient.
     *
     * @return {@link DeliveryResult#delivered() delivered}, a
     *     {@link DeliveryResult#transientFailure transient failure} (the application retries later, after
     *     the given retry-after time if one is set, for example from an HTTP 429 {@code Retry-After}
     *     header), or a {@link DeliveryResult#permanentFailure permanent failure} (the address no longer
     *     works, for example Slack answers 404 or 410; the application stops sending to this subscriber and
     *     marks it inactive, FR-21). A runtime exception thrown by this method is treated like a transient
     *     failure.
     */
    DeliveryResult send(M message, Recipient recipient);

    /**
     * The limits the application must respect when calling {@link #send} (for example the email service's
     * send rate, or one message per second per Slack webhook). Read once at start-up.
     */
    PacingLimits pacingLimits();

    /** Renders and sends in one step; what the delivery service normally calls. */
    default DeliveryResult deliver(NotificationContent content, Recipient recipient) {
        return send(render(content, recipient), recipient);
    }
}
