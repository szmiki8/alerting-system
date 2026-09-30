package com.sonrisa.alerting.app.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonrisa.alerting.app.AlertingApplication;
import com.sonrisa.alerting.app.config.DeployedProfileArguments;
import com.sonrisa.alerting.spi.subscriber.FieldError;
import com.sonrisa.alerting.spi.subscriber.SubscriberInput;
import com.sonrisa.alerting.spi.subscriber.SubscriberType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The real Slack plugin ({@code channel-slack}, a runtime dependency) in the real application (BE-18). Like the
 * email plugin, it registers the {@code slack} subscriber type whenever it is enabled, and the application refuses
 * to start when there is no {@code slack} channel (OP-21). The real channel follows in BE-34, so these tests supply
 * the test-only one from {@code com.sonrisa.alerting.testplugin}. The type's behaviour is tested in the plugin
 * module.
 */
@ExtendWith(OutputCaptureExtension.class)
class SlackPluginStartupTest {

    private static final String WEBHOOK = "https://hooks.slack.com/services/" + "T0TESTTEAM/B0TESTHOOK/"
            + "abcdEFGH1234" + "ijklMNOP5678";

    private static ConfigurableApplicationContext start(String... properties) {
        return startWithProfile("test", properties);
    }

    private static ConfigurableApplicationContext startWithProfile(String profile, String... properties) {
        List<String> args = new ArrayList<>(List.of("--spring.profiles.active=" + profile,
                "--server.port=0", "--management.server.port=0",
                // Plain logs: a failed start does not reset the JSON log format for later tests in this JVM.
                "--logging.structured.format.console="));
        for (String property : properties) {
            args.add("--" + property);
        }
        return SpringApplication.run(AlertingApplication.class, args.toArray(String[]::new));
    }

    @Test
    void slackSubscriberTypeIsRegisteredWithASlackChannel() {
        try (ConfigurableApplicationContext context = start(
                "alerting.channels.slack.enabled=true",
                "alerting.channels.test-slack.enabled=true")) {

            assertThat(context.getBean(NotificationChannelRegistry.class).keys()).containsExactly("slack");
            SubscriberType slack = context.getBean(SubscriberTypeRegistry.class).get("slack");
            assertThat(slack.channelKey()).isEqualTo("slack");
            assertThat(slack.addressIsSecret()).isTrue();
            assertThat(slack.validate(new SubscriberInput("Team", WEBHOOK))).isEmpty();
            assertThat(slack.validate(new SubscriberInput("Team", "https://evil.example/hook")))
                    .extracting(FieldError::field).containsExactly(SubscriberInput.FIELD_ADDRESS);
            assertThat(slack.mask(slack.normalise(new SubscriberInput(null, " " + WEBHOOK + "/"))))
                    .isEqualTo("https://hooks.slack.com/services/...5678");
        }
    }

    @Test
    void slackTypeWithoutSlackChannelStopsTheApplication(CapturedOutput output) {
        // The situation until BE-34 adds the real channel: the flag must stay off (OP-21).
        assertThatThrownBy(() -> start("alerting.channels.slack.enabled=true"))
                .isInstanceOf(Exception.class);

        assertThat(output.getOut() + output.getErr())
                .contains("APPLICATION FAILED TO START")
                .contains("The subscriber type 'slack'")
                .contains("alerting.channels.slack.enabled=true");
    }

    /** The Slack plugin is off unless enabled (OP-22), so every profile starts without a channel for it. */
    @ParameterizedTest
    @ValueSource(strings = {"local", "demo", "test"})
    void profilesStartWithoutTheSlackType(String profile) {
        List<String> properties = new ArrayList<>(List.of("alerting.management.operator-password=test-only-password"));
        for (String key : DeployedProfileArguments.KEYS) {
            properties.add(key.substring(2));
        }
        try (ConfigurableApplicationContext context = startWithProfile(profile, properties.toArray(String[]::new))) {
            assertThat(context.getBean(SubscriberTypeRegistry.class).contains("slack")).isFalse();
        }
    }
}
