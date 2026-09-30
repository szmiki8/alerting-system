package com.sonrisa.alerting.testplugin;

import com.sonrisa.alerting.spi.channel.NotificationChannel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;

/**
 * A test-only {@code slack} channel, standing in for the Slack channel of BE-34, so that the real {@code slack}
 * subscriber type of {@code channel-slack} can start (BE-18, BE-19): without a {@code slack} channel the
 * application refuses to start (OP-21). Enabled together with {@code alerting.channels.slack.enabled}. BE-34
 * removes this class.
 */
@AutoConfiguration
@ConditionalOnBooleanProperty("alerting.channels.test-slack.enabled")
public class TestSlackChannelAutoConfiguration {

    @Bean
    NotificationChannel<String> testSlackChannel() {
        return new TestChannel("slack");
    }
}
