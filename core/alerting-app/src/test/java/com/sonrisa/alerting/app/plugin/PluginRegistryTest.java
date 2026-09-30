package com.sonrisa.alerting.app.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonrisa.alerting.testplugin.TestChannel;
import com.sonrisa.alerting.testplugin.TestSource;
import com.sonrisa.alerting.testplugin.TestSubscriberType;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class PluginRegistryTest {

    @Test
    void pluginsAreFoundByKeyAndListedInKeyOrder() {
        TestSource news = new TestSource("news");
        TestSource stub = new TestSource("stub");
        EventSourceRegistry registry = new EventSourceRegistry(List.of(stub, news));

        assertThat(registry.keys()).containsExactly("news", "stub");
        assertThat(registry.all()).containsExactly(news, stub);
        assertThat(registry.get("stub")).isSameAs(stub);
        assertThat(registry.find("stub")).containsSame(stub);
        assertThat(registry.find("other")).isEmpty();
        assertThat(registry.contains("news")).isTrue();
        assertThatIllegalArgumentException().isThrownBy(() -> registry.get("other"))
                .withMessageContaining("No enabled event source with key 'other'");
    }

    @Test
    void noPluginsGiveAnEmptyRegistry() {
        assertThat(new NotificationChannelRegistry(List.of()).keys()).isEmpty();
    }

    @Test
    void duplicateKeyIsRejectedWithBothClassNames() {
        TestChannel first = new TestChannel("email");
        TestChannel second = new TestChannel("email") {
        };

        assertThatThrownBy(() -> new NotificationChannelRegistry(List.of(first, second)))
                .isInstanceOf(PluginConfigurationException.class)
                .hasMessageContaining("Two notification channels have the key 'email'")
                .hasMessageContaining(TestChannel.class.getName())
                .hasMessageContaining(second.getClass().getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Email", "1email", "e mail", "email_2", "-email"})
    void invalidKeyIsRejected(String key) {
        assertThatThrownBy(() -> new EventSourceRegistry(List.of(new TestSource(key))))
                .isInstanceOf(PluginConfigurationException.class)
                .hasMessageContaining("invalid key '" + key + "'");
    }

    @Test
    void sameKeyInDifferentExtensionPointsIsAllowed() {
        NotificationChannelRegistry channels = new NotificationChannelRegistry(List.of(new TestChannel("email")));
        SubscriberTypeRegistry types = new SubscriberTypeRegistry(
                List.of(new TestSubscriberType("email", "email")), channels);

        assertThat(types.keys()).containsExactly("email");
    }

    @Test
    void subscriberTypeWithoutItsChannelIsRejected() {
        NotificationChannelRegistry channels = new NotificationChannelRegistry(List.of(new TestChannel("log")));

        assertThatThrownBy(() -> new SubscriberTypeRegistry(
                List.of(new TestSubscriberType("slack", "slack")), channels))
                .isInstanceOf(PluginConfigurationException.class)
                .hasMessageContaining("subscriber type 'slack'")
                .hasMessageContaining("channel 'slack'")
                .hasMessageContaining("Enabled channels: [log]")
                .hasMessageContaining("alerting.channels.slack.enabled=true");
    }
}
