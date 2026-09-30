package com.sonrisa.alerting.channel.slack;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonrisa.alerting.spi.subscriber.SubscriberType;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

/**
 * The plugin's registration: off unless enabled; when enabled, the webhook client and the subscriber type, with no
 * further condition. Whether a {@code slack} channel exists is checked by the application's subscriber type
 * registry, which stops the start-up without one (OP-21, see {@code SlackPluginStartupTest} in the application).
 */
class SlackAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpClientAutoConfiguration.class,
                    ImperativeHttpClientAutoConfiguration.class, RestClientAutoConfiguration.class,
                    ValidationAutoConfiguration.class, SlackAutoConfiguration.class));

    @Test
    void offUnlessEnabled() {
        runner.run(context -> assertThat(context).hasNotFailed()
                .doesNotHaveBean(SlackWebhookClient.class)
                .doesNotHaveBean(SubscriberType.class)
                .doesNotHaveBean(SlackProperties.class));
        runner.withPropertyValues("alerting.channels.slack.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(SlackWebhookClient.class));
    }

    @Test
    void enabledRegistersClientAndSubscriberTypeWithDefaultTimeouts() {
        runner.withPropertyValues("alerting.channels.slack.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed()
                            .hasSingleBean(SlackWebhookClient.class)
                            .hasSingleBean(SlackSubscriberType.class);
                    assertThat(context.getBean(SubscriberType.class).key()).isEqualTo("slack");
                    SlackProperties.Timeouts timeouts = context.getBean(SlackProperties.class).verification();
                    assertThat(timeouts.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
                    assertThat(timeouts.readTimeout()).isEqualTo(Duration.ofSeconds(5));
                });
    }

    @Test
    void timeoutsAreConfigurable() {
        runner.withPropertyValues("alerting.channels.slack.enabled=true",
                        "alerting.channels.slack.verification.connect-timeout=1s",
                        "alerting.channels.slack.verification.read-timeout=3s")
                .run(context -> {
                    SlackProperties.Timeouts timeouts = context.getBean(SlackProperties.class).verification();
                    assertThat(timeouts.connectTimeout()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(timeouts.readTimeout()).isEqualTo(Duration.ofSeconds(3));
                });
    }

    @Test
    void invalidTimeoutsStopTheStartUp() {
        runner.withPropertyValues("alerting.channels.slack.enabled=true",
                        "alerting.channels.slack.verification.read-timeout=0s")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("alerting.channels.slack.enabled=true",
                        "alerting.channels.slack.verification.connect-timeout=5m")
                .run(context -> assertThat(context).hasFailed());
    }
}
