package com.sonrisa.alerting.channel.slack;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * The Slack plugin (plugin module convention, see {@code com.sonrisa.alerting.app.plugin}): off unless
 * {@code alerting.channels.slack.enabled=true} (OP-22).
 *
 * <p>The {@code slack} subscriber type shares the channel's flag, as the plugin module convention prescribes.
 * The Slack notification channel follows in BE-34 as another bean of this class. Until then the flag must stay
 * off in every profile: the application refuses to start with a subscriber type whose channel is missing
 * (OP-21), exactly like the email plugin.
 */
@AutoConfiguration
@ConditionalOnBooleanProperty("alerting.channels.slack.enabled")
@EnableConfigurationProperties(SlackProperties.class)
public class SlackAutoConfiguration {

    /** The webhook client for the welcome message, with the short verification timeouts. */
    @Bean
    SlackWebhookClient slackVerificationClient(ObjectProvider<RestClient.Builder> restClientBuilders,
            ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder, HttpClientSettings globalSettings,
            SlackProperties properties) {
        SlackProperties.Timeouts timeouts = properties.verification();
        return new SlackWebhookClient(restClientBuilders.getObject(), requestFactoryBuilder, globalSettings,
                timeouts.connectTimeout(), timeouts.readTimeout());
    }

    @Bean
    SlackSubscriberType slackSubscriberType(SlackWebhookClient slackVerificationClient) {
        return new SlackSubscriberType(slackVerificationClient);
    }
}
