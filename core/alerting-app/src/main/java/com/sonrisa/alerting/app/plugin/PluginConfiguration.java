package com.sonrisa.alerting.app.plugin;

import com.sonrisa.alerting.spi.channel.NotificationChannel;
import com.sonrisa.alerting.spi.source.EventSource;
import com.sonrisa.alerting.spi.subscriber.SubscriberType;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Collects the plugin beans into the three registries and logs the enabled keys. Which plugins exist is
 * decided only by the plugin modules' auto-configurations and their {@code enabled} properties; this
 * class never names a plugin.
 */
@Configuration(proxyBeanMethods = false)
public class PluginConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(PluginConfiguration.class);

    @Bean
    EventSourceRegistry eventSourceRegistry(ObjectProvider<EventSource> sources) {
        EventSourceRegistry registry = new EventSourceRegistry(list(sources));
        LOG.info("Enabled event sources: {}", registry.keys());
        return registry;
    }

    @Bean
    NotificationChannelRegistry notificationChannelRegistry(ObjectProvider<NotificationChannel<?>> channels) {
        NotificationChannelRegistry registry = new NotificationChannelRegistry(list(channels));
        LOG.info("Enabled notification channels: {}", registry.keys());
        return registry;
    }

    @Bean
    SubscriberTypeRegistry subscriberTypeRegistry(ObjectProvider<SubscriberType> types,
            NotificationChannelRegistry channels) {
        SubscriberTypeRegistry registry = new SubscriberTypeRegistry(list(types), channels);
        LOG.info("Enabled subscriber types: {}", registry.keys());
        return registry;
    }

    /** All beans of the type, also none (then there is no dependency error, just an empty registry). */
    private static <T> List<T> list(ObjectProvider<T> beans) {
        return beans.orderedStream().toList();
    }
}
