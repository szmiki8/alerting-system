package com.sonrisa.alerting.testplugin;

import com.sonrisa.alerting.spi.source.EventSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;

/** A source plugin registered the way a plugin module does it: auto-configuration, off unless enabled. */
@AutoConfiguration
@ConditionalOnBooleanProperty("alerting.sources.test-source.enabled")
public class TestSourceAutoConfiguration {

    @Bean
    EventSource testSource() {
        return new TestSource("test-source");
    }
}
