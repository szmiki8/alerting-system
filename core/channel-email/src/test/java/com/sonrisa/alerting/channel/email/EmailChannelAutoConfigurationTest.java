package com.sonrisa.alerting.channel.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonrisa.alerting.spi.subscriber.SubscriberType;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class EmailChannelAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EmailChannelAutoConfiguration.class));

    @Test
    void isOffUnlessEnabled() {
        runner.run(context -> assertThat(context).doesNotHaveBean(SubscriberType.class));
        runner.withPropertyValues("alerting.channels.email.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(SubscriberType.class));
    }

    @Test
    void registersTheEmailSubscriberTypeWhenEnabled() {
        runner.withPropertyValues("alerting.channels.email.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(SubscriberType.class);
            assertThat(context.getBean(SubscriberType.class))
                    .isInstanceOf(EmailSubscriberType.class)
                    .extracting(SubscriberType::key, SubscriberType::channelKey)
                    .containsExactly("email", "email");
        });
    }

    @Test
    void isListedInTheAutoConfigurationImports() throws Exception {
        String resource = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).isNotNull();
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .contains(EmailChannelAutoConfiguration.class.getName());
        }
    }
}
