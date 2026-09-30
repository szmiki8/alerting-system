package com.sonrisa.alerting.app.plugin;

import com.sonrisa.alerting.spi.subscriber.SubscriberType;
import java.util.List;

/**
 * The enabled {@link SubscriberType}s by key. Every registered type has an enabled channel: a type whose
 * {@link SubscriberType#channelKey() channel} is missing or disabled stops the start-up (fail, not warn),
 * because its subscribers could sign up but would never receive a notification.
 */
public final class SubscriberTypeRegistry extends PluginRegistry<SubscriberType> {

    /**
     * @throws PluginConfigurationException on an invalid or duplicate key, or when a type's channel is not
     *     in {@code channels}
     */
    public SubscriberTypeRegistry(List<? extends SubscriberType> types, NotificationChannelRegistry channels) {
        super("subscriber type", types, SubscriberType::key);
        for (SubscriberType type : all()) {
            String channelKey = type.channelKey();
            if (!channels.contains(channelKey)) {
                throw new PluginConfigurationException(String.format(
                        "The subscriber type '%s' (%s) is delivered by the channel '%s', but no enabled"
                                + " notification channel has that key. Enabled channels: %s. Enable the channel"
                                + " (alerting.channels.%s.enabled=true) or disable the subscriber type.",
                        type.key(), type.getClass().getName(), channelKey, channels.keys(), channelKey));
            }
        }
    }
}
