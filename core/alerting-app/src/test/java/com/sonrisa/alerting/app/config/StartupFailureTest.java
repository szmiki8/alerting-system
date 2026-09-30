package com.sonrisa.alerting.app.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonrisa.alerting.app.AlertingApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** The real application refuses to start on an invalid value and names the property in the report. */
@ExtendWith(OutputCaptureExtension.class)
class StartupFailureTest {

    @Test
    void invalidCronStopsStartupWithClearMessage(CapturedOutput output) {
        assertThatThrownBy(() -> SpringApplication.run(AlertingApplication.class,
                "--spring.profiles.active=test",
                "--server.port=0", "--management.server.port=0",
                "--alerting.schedule.collection-cron=every hour"))
                .isInstanceOf(Exception.class);

        assertThat(output.getOut() + output.getErr())
                .contains("APPLICATION FAILED TO START")
                .contains("alerting.schedule.collection-cron")
                .contains("must be a valid Spring cron expression");
    }

    @ParameterizedTest
    @ValueSource(strings = {"demo", "postgres", "aws"})
    void deployedProfileRefusesToStartWithoutOperatorPassword(String profile, CapturedOutput output) {
        assertThatThrownBy(() -> SpringApplication.run(AlertingApplication.class,
                DeployedProfileArguments.withDatabase(profile, DeployedProfileArguments.concat(new String[] {
                        "--spring.profiles.active=" + profile,
                        "--server.port=0", "--management.server.port=0",
                        // Plain logs: a failed start does not reset the JSON log format for later tests in this JVM.
                        "--logging.structured.format.console="},
                        // Only the password is missing.
                        DeployedProfileArguments.KEYS))))
                .isInstanceOf(Exception.class);

        assertThat(output.getOut() + output.getErr())
                .contains("APPLICATION FAILED TO START")
                .contains("alerting.management")
                .contains("must be set through ALERTING_MANAGEMENT_OPERATOR_PASSWORD in this profile");
    }

    @ParameterizedTest
    @ValueSource(strings = {"demo", "postgres", "aws"})
    void deployedProfileRefusesToStartWithoutKeys(String profile, CapturedOutput output) {
        assertThatThrownBy(() -> SpringApplication.run(AlertingApplication.class,
                DeployedProfileArguments.withDatabase(profile,
                        "--spring.profiles.active=" + profile,
                        "--server.port=0", "--management.server.port=0",
                        "--logging.structured.format.console=",
                        // Only the keys are missing (BE-09).
                        "--alerting.management.operator-password=test-only-password")))
                .isInstanceOf(Exception.class);

        assertThat(output.getOut() + output.getErr())
                .contains("APPLICATION FAILED TO START")
                .contains("alerting.security")
                .contains("must be set through ALERTING_SECURITY_ENCRYPTION_KEY and ALERTING_SECURITY_FINGERPRINT_KEY"
                        + " in this profile");
    }
}
