package com.sonrisa.alerting.testplugin;

import com.sonrisa.alerting.spi.channel.NotificationChannel;
import com.sonrisa.alerting.spi.subscriber.SubscriberType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;

/** A channel module: the channel and the subscriber type it delivers to, under one enabled flag. */
@AutoConfiguration
@ConditionalOnBooleanProperty("alerting.channels.test-channel.enabled")
public class TestChannelAutoConfiguration {

    @Bean
    NotificationChannel<String> testChannel() {
        return new TestChannel("test-channel");
    }

    @Bean
    SubscriberType testSubscriberType() {
        return new TestSubscriberType("test-channel", "test-channel");
    }
}
