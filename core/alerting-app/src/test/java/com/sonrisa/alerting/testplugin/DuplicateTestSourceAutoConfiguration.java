package com.sonrisa.alerting.testplugin;

import com.sonrisa.alerting.spi.source.EventSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;

/** A second module that wrongly claims the key {@code test-source}. */
@AutoConfiguration
@ConditionalOnBooleanProperty("alerting.sources.test-source-copy.enabled")
public class DuplicateTestSourceAutoConfiguration {

    @Bean
    EventSource testSourceCopy() {
        return new CopiedTestSource();
    }

    /** Its own class, so that the failure message names two different classes. */
    static class CopiedTestSource extends TestSource {

        CopiedTestSource() {
            super("test-source");
        }
    }
}
