package com.sonrisa.alerting.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonrisa.alerting.app.AlertingApplication;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;

/** Logs are JSON (ECS) in demo, postgres and aws; human-readable in local. */
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingTest {

    private static String startAndCapture(String profile, CapturedOutput output) {
        ConfigurableApplicationContext context = SpringApplication.run(AlertingApplication.class,
                "--spring.profiles.active=" + profile,
                "--server.port=0", "--management.server.port=0",
                // The deployed profiles require an operator password (test-only value).
                "--alerting.management.operator-password=test-only-password");
        context.close();
        return output.getOut();
    }

    @ParameterizedTest
    @ValueSource(strings = {"demo", "postgres", "aws"})
    void logsJsonInDeploymentProfiles(String profile, CapturedOutput output) {
        String out = startAndCapture(profile, output);

        assertThat(out.lines().filter(line -> line.contains("profile is active")))
                .singleElement()
                .satisfies(line -> assertThat(line).startsWith("{").contains("\"ecs\":{\"version\"")
                        .contains("\"log\":{\"level\":\"INFO\""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"local"})
    void logsPlainTextLocally(String profile, CapturedOutput output) {
        String out = startAndCapture(profile, output);

        assertThat(out.lines().filter(line -> line.contains("profile is active")))
                .singleElement()
                .satisfies(line -> assertThat(line).doesNotStartWith("{").contains("INFO"));
    }
}
