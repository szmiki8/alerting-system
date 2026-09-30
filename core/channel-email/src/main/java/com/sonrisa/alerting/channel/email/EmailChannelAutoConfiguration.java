package com.sonrisa.alerting.channel.email;

import com.sonrisa.alerting.spi.subscriber.SubscriberType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;

/**
 * Registers the email plugin when {@code alerting.channels.email.enabled=true} (off by default, OP-22). The
 * subscriber type shares the channel's flag, as the plugin module convention prescribes.
 *
 * <p>BE-15 provides only the subscriber type. The {@code email} notification channel follows in BE-33 as a
 * second bean of this class, together with the module's {@code @ConfigurationProperties} (gateway settings).
 * Until then the flag must stay off in every profile: the application refuses to start with a subscriber type
 * whose channel is missing (OP-21).
 */
@AutoConfiguration
@ConditionalOnBooleanProperty("alerting.channels.email.enabled")
public class EmailChannelAutoConfiguration {

    @Bean
    SubscriberType emailSubscriberType() {
        return new EmailSubscriberType();
    }
}
