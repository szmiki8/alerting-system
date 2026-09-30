package com.sonrisa.alerting.app.plugin;

import com.sonrisa.alerting.spi.channel.NotificationChannel;
import java.util.List;

/** The enabled {@link NotificationChannel}s by key. */
public final class NotificationChannelRegistry extends PluginRegistry<NotificationChannel<?>> {

    public NotificationChannelRegistry(List<? extends NotificationChannel<?>> channels) {
        super("notification channel", channels, NotificationChannel::key);
    }
}
