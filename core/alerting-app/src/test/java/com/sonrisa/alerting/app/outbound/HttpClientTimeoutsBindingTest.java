package com.sonrisa.alerting.app.outbound;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/** Timeouts come from each integration's own configuration group and are required there (BE-17). */
class HttpClientTimeoutsBindingTest {

    /** Stands in for a source's configuration group, e.g. alerting.sources.newsapi. */
    @Validated
    @ConfigurationProperties("alerting.sources.fake")
    record FakeSourceProperties(@Valid @NotNull HttpClientTimeouts http) {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FakeSourceProperties.class)
    static class FakeSourceConfiguration {
    }

    final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(FakeSourceConfiguration.class);

    @Test
    void bindsTimeoutsFromTheIntegrationsGroup() {
        runner.withPropertyValues("alerting.sources.fake.http.connect-timeout=2s",
                        "alerting.sources.fake.http.read-timeout=5s")
                .run(context -> assertThat(context.getBean(FakeSourceProperties.class).http())
                        .isEqualTo(new HttpClientTimeouts(Duration.ofSeconds(2), Duration.ofSeconds(5))));
    }

    @Test
    void missingReadTimeoutFailsTheStart() {
        runner.withPropertyValues("alerting.sources.fake.http.connect-timeout=2s")
                .run(context -> assertThat(context).getFailure().rootCause()
                        .hasMessageContaining("alerting.sources.fake.http.readTimeout"));
    }

    @Test
    void missingGroupFailsTheStart() {
        runner.run(context -> assertThat(context).getFailure().rootCause()
                .hasMessageContaining("alerting.sources.fake.http"));
    }
}
