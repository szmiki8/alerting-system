package com.sonrisa.alerting.testplugin;

import com.sonrisa.alerting.spi.subscriber.SubscriberType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;

/** A module with only a subscriber type that relies on the {@code test-channel} channel of another module. */
@AutoConfiguration
@ConditionalOnBooleanProperty("alerting.channels.test-orphan.enabled")
public class OrphanSubscriberTypeAutoConfiguration {

    @Bean
    SubscriberType orphanSubscriberType() {
        return new TestSubscriberType("test-orphan", "test-channel");
    }
}
