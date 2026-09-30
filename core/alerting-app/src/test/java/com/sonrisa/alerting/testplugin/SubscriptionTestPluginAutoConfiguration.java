package com.sonrisa.alerting.testplugin;

import com.sonrisa.alerting.spi.channel.NotificationChannel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;

/**
 * A channel module with two fake subscriber types for the subscription service tests (BE-14): {@code test-plain}
 * (address stored in plain form) and {@code test-secret} (address is a secret, stored encrypted).
 */
@AutoConfiguration
@ConditionalOnBooleanProperty("alerting.channels.test-subscription.enabled")
public class SubscriptionTestPluginAutoConfiguration {

    @Bean
    NotificationChannel<String> testSubscriptionChannel() {
        return new TestChannel("test-subscription");
    }

    @Bean
    FakeSubscriberType testPlainSubscriberType() {
        return new FakeSubscriberType("test-plain", "test-subscription", false);
    }

    @Bean
    FakeSubscriberType testSecretSubscriberType() {
        return new FakeSubscriberType("test-secret", "test-subscription", true);
    }
}
