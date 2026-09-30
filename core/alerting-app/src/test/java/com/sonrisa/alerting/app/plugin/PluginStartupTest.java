package com.sonrisa.alerting.app.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonrisa.alerting.app.AlertingApplication;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Starts the real application with the test plugins from {@code com.sonrisa.alerting.testplugin}, which
 * are registered through the test resources' auto-configuration imports file and switched by their
 * {@code enabled} properties, exactly as the plugin modules will be.
 */
@ExtendWith(OutputCaptureExtension.class)
class PluginStartupTest {

    private static ConfigurableApplicationContext start(String... properties) {
        List<String> args = new ArrayList<>(List.of("--spring.profiles.active=test",
                "--server.port=0", "--management.server.port=0"));
        for (String property : properties) {
            args.add("--" + property);
        }
        return SpringApplication.run(AlertingApplication.class, args.toArray(String[]::new));
    }

    @Test
    void enabledPluginsAreRegisteredAndTheirKeysLogged(CapturedOutput output) {
        try (ConfigurableApplicationContext context = start(
                "alerting.sources.test-source.enabled=true",
                "alerting.channels.test-channel.enabled=true")) {

            assertThat(context.getBean(EventSourceRegistry.class).keys()).containsExactly("test-source");
            assertThat(context.getBean(NotificationChannelRegistry.class).keys()).containsExactly("test-channel");
            assertThat(context.getBean(SubscriberTypeRegistry.class).keys()).containsExactly("test-channel");
        }

        assertThat(output.getOut())
                .contains("Enabled event sources: [test-source]")
                .contains("Enabled notification channels: [test-channel]")
                .contains("Enabled subscriber types: [test-channel]");
    }

    @Test
    void pluginsAreOffUnlessEnabledAndDisablingRemovesThem() {
        try (ConfigurableApplicationContext context = start(
                "alerting.sources.test-source.enabled=false")) {

            assertThat(context.getBean(EventSourceRegistry.class).keys()).isEmpty();
            // Not mentioned at all: off by default.
            assertThat(context.getBean(NotificationChannelRegistry.class).keys()).isEmpty();
            assertThat(context.getBean(SubscriberTypeRegistry.class).keys()).isEmpty();
        }
    }

    @Test
    void duplicateKeyStopsTheApplicationWithClearMessage(CapturedOutput output) {
        assertThatThrownBy(() -> start(
                "alerting.sources.test-source.enabled=true",
                "alerting.sources.test-source-copy.enabled=true"))
                .isInstanceOf(Exception.class);

        assertThat(output.getOut() + output.getErr())
                .contains("APPLICATION FAILED TO START")
                .contains("Invalid plugin configuration: Two event sources have the key 'test-source'");
    }

    @Test
    void subscriberTypeWithDisabledChannelStopsTheApplication(CapturedOutput output) {
        assertThatThrownBy(() -> start(
                "alerting.channels.test-orphan.enabled=true",
                "alerting.channels.test-channel.enabled=false"))
                .isInstanceOf(Exception.class);

        assertThat(output.getOut() + output.getErr())
                .contains("APPLICATION FAILED TO START")
                .contains("The subscriber type 'test-orphan'")
                .contains("alerting.channels.test-channel.enabled=true");
    }
}
