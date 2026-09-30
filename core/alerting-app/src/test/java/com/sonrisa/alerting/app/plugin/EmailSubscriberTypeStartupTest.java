package com.sonrisa.alerting.app.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonrisa.alerting.app.AlertingApplication;
import com.sonrisa.alerting.app.config.DeployedProfileArguments;
import com.sonrisa.alerting.app.persistence.crypto.AddressFingerprinter;
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
 * The real {@code email} subscriber type from {@code channel-email} (BE-15), a runtime-only plugin, in the running
 * application. The real email channel follows in BE-33, so these tests supply a test-only channel with the key
 * {@code email} ({@code com.sonrisa.alerting.testplugin.TestEmailChannelAutoConfiguration}).
 */
@ExtendWith(OutputCaptureExtension.class)
class EmailSubscriberTypeStartupTest {

    private static ConfigurableApplicationContext start(String profile, String... properties) {
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
    void emailTypeIsRegisteredWhenEnabledWithAnEmailChannel() {
        try (ConfigurableApplicationContext context = start("test",
                "alerting.channels.email.enabled=true",
                "alerting.channels.test-email.enabled=true")) {

            SubscriberTypeRegistry types = context.getBean(SubscriberTypeRegistry.class);
            assertThat(types.keys()).containsExactly("email");
            SubscriberType email = types.get("email");
            assertThat(email.channelKey()).isEqualTo("email");
            assertThat(email.addressIsSecret()).isFalse();

            // Same normalised address, hence the same fingerprint (FR-05).
            AddressFingerprinter fingerprinter = context.getBean(AddressFingerprinter.class);
            String mixedCase = email.normalise(new SubscriberInput("Alice", "Alice@Example.COM ")).value();
            String lowerCase = email.normalise(new SubscriberInput("Alice", "alice@example.com")).value();
            assertThat(mixedCase).isEqualTo(lowerCase).isEqualTo("alice@example.com");
            assertThat(fingerprinter.fingerprint(mixedCase)).isEqualTo(fingerprinter.fingerprint(lowerCase));
        }
    }

    @Test
    void emailTypeWithoutEmailChannelStopsTheApplication(CapturedOutput output) {
        // The situation until BE-33 adds the real channel: the flag must stay off (OP-21).
        assertThatThrownBy(() -> start("test", "alerting.channels.email.enabled=true"))
                .isInstanceOf(Exception.class);

        assertThat(output.getOut() + output.getErr())
                .contains("APPLICATION FAILED TO START")
                .contains("The subscriber type 'email'")
                .contains("alerting.channels.email.enabled=true");
    }

    /** The email plugin is off unless enabled (OP-22), so every profile starts without a channel for it. */
    @ParameterizedTest
    @ValueSource(strings = {"local", "demo", "test"})
    void profilesStartWithoutTheEmailType(String profile) {
        List<String> properties = new ArrayList<>(List.of("alerting.management.operator-password=test-only-password"));
        for (String key : DeployedProfileArguments.KEYS) {
            properties.add(key.substring(2));
        }
        try (ConfigurableApplicationContext context = start(profile, properties.toArray(String[]::new))) {
            assertThat(context.getBean(SubscriberTypeRegistry.class).contains("email")).isFalse();
        }
    }
}
