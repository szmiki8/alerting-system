package com.sonrisa.alerting.testplugin;

import com.sonrisa.alerting.spi.channel.NotificationChannel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;

/**
 * A stand-in {@code email} channel, so that the real email subscriber type from {@code channel-email} (BE-15)
 * can start before the real email channel exists (BE-33). Enabled together with
 * {@code alerting.channels.email.enabled}.
 */
@AutoConfiguration
@ConditionalOnBooleanProperty("alerting.channels.test-email.enabled")
public class TestEmailChannelAutoConfiguration {

    @Bean
    NotificationChannel<String> testEmailChannel() {
        return new TestChannel("email");
    }
}
