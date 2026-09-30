/**
 * Extension interfaces of the alerting core (architecture Section 7, ADR-10).
 *
 * <p>Three extension points, each in its own package:
 * <ul>
 *   <li>{@link com.sonrisa.alerting.spi.source.EventSource}: fetches items from one external service
 *       and returns them as event drafts in the standard event format;</li>
 *   <li>{@link com.sonrisa.alerting.spi.subscriber.SubscriberType}: everything that differs between kinds
 *       of subscribers (validation, normalisation, verification, masking) and the channel that delivers
 *       to them;</li>
 *   <li>{@link com.sonrisa.alerting.spi.channel.NotificationChannel}: renders and sends one notification
 *       (one event to one recipient) through one medium.</li>
 * </ul>
 *
 * <p><b>Keys.</b> Every implementation has a key: lower-case letters, digits and hyphens, starting with a
 * letter (for example {@code newsapi}, {@code email}, {@code slack}). The key is stored in the database
 * and names the plugin's configuration group, so it must never change once in use. Keys are unique per
 * extension point; the application refuses to start if two implementations of the same interface claim
 * the same key. A subscriber type and a channel may share a key (for example {@code email}).
 *
 * <p><b>Registration.</b> Implementations are Spring beans in a plugin module. The module registers a
 * Spring Boot auto-configuration (listed in
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}) that is active
 * only when {@code alerting.sources.<key>.enabled=true} (sources) or
 * {@code alerting.channels.<key>.enabled=true} (channels and the subscriber types they deliver to).
 * Plugins are off unless enabled. The module's own settings live under the same group. This package
 * itself has no Spring dependency.
 *
 * <p><b>Threading.</b> Implementations must be thread-safe: the application may call them from several
 * worker threads at once.
 *
 * <p><b>Nullness.</b> Everything is non-null unless annotated with {@link org.jspecify.annotations.Nullable}.
 */
@NullMarked
package com.sonrisa.alerting.spi;

import org.jspecify.annotations.NullMarked;
