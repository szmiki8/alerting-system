package com.sonrisa.alerting.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ConfigurationPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AlertingConfiguration.class);

    @Test
    void bindsDefaults() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            ScheduleProperties schedule = context.getBean(ScheduleProperties.class);
            assertThat(schedule.collectionCron()).isEqualTo("0 0 * * * *");
            assertThat(schedule.lockAtMostFor()).isEqualTo(Duration.ofMinutes(55));
            assertThat(context.getBean(DeliveryProperties.class).workerPoolSize()).isEqualTo(4);
            RetentionProperties retention = context.getBean(RetentionProperties.class);
            assertThat(retention.events()).isEqualTo(Duration.ofDays(30));
            assertThat(retention.runs()).isEqualTo(Duration.ofDays(90));
            assertThat(retention.audit()).isEqualTo(Duration.ofDays(365));
            AlertingSecurityProperties security = context.getBean(AlertingSecurityProperties.class);
            assertThat(security.sessionTimeout()).isEqualTo(Duration.ofMinutes(30));
            assertThat(security.adminAllowList()).isEmpty();
            assertThat(context.getBean(AlertingManagementProperties.class).operatorUsername()).isEqualTo("operator");
        });
    }

    @Test
    void zoneDefaultsToBudapestAndIsOneBean() {
        runner.run(context -> {
            assertThat(context).getBean(ZoneId.class).isEqualTo(ZoneId.of("Europe/Budapest"));
            assertThat(context.getBeansOfType(ZoneId.class)).hasSize(1);
        });
    }

    @Test
    void zoneCanBeConfigured() {
        runner.withPropertyValues("alerting.schedule.zone=Europe/Vienna")
                .run(context -> assertThat(context).getBean(ZoneId.class).isEqualTo(ZoneId.of("Europe/Vienna")));
    }

    @Test
    void bindsAllowListFromCommaSeparatedValue() {
        runner.withPropertyValues("alerting.security.admin-allow-list=a@example.org,b@example.org")
                .run(context -> assertThat(context.getBean(AlertingSecurityProperties.class).adminAllowList())
                        .containsExactly("a@example.org", "b@example.org"));
    }

    @Test
    void rejectsInvalidCron() {
        runner.withPropertyValues("alerting.schedule.collection-cron=every hour")
                .run(context -> assertThat(context).getFailure()
                        .rootCause()
                        .hasMessageContaining("alerting.schedule")
                        .hasMessageContaining("collectionCron")
                        .hasMessageContaining("must be a valid Spring cron expression"));
    }

    @Test
    void rejectsUnknownZone() {
        runner.withPropertyValues("alerting.schedule.zone=Mars/Olympus")
                .run(context -> assertThat(context).getFailure()
                        .hasStackTraceContaining("Failed to bind properties under 'alerting.schedule.zone'"));
    }

    @Test
    void rejectsOutOfRangeValues() {
        runner.withPropertyValues("alerting.delivery.worker-pool-size=0")
                .run(context -> assertThat(context).getFailure().rootCause().hasMessageContaining("workerPoolSize"));
        runner.withPropertyValues("alerting.retention.events=1h")
                .run(context -> assertThat(context).getFailure().rootCause().hasMessageContaining("events"));
        runner.withPropertyValues("alerting.security.session-timeout=10s")
                .run(context -> assertThat(context).getFailure().rootCause().hasMessageContaining("sessionTimeout"));
    }

    @Test
    void rejectsInvalidAllowListEntry() {
        runner.withPropertyValues("alerting.security.admin-allow-list=not-an-email")
                .run(context -> assertThat(context).getFailure().rootCause().hasMessageContaining("adminAllowList"));
    }

    @Test
    void toStringNeverShowsSecrets() {
        runner.withPropertyValues(
                        "alerting.security.encryption-key=test-only-encryption-key",
                        "alerting.security.fingerprint-key=test-only-fingerprint-key",
                        "alerting.management.operator-password=test-only-password")
                .run(context -> {
                    assertThat(context.getBean(AlertingSecurityProperties.class).toString())
                            .doesNotContain("test-only").contains("encryptionKey=<set>");
                    assertThat(context.getBean(AlertingManagementProperties.class).toString())
                            .doesNotContain("test-only").contains("operatorPassword=<set>");
                });
    }

    @Test
    void operatorPasswordIsOptionalByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(AlertingManagementProperties.class).operatorPasswordRequired()).isFalse();
        });
    }

    @Test
    void rejectsMissingOperatorPasswordWhenRequired() {
        runner.withPropertyValues("alerting.management.operator-password-required=true")
                .run(context -> assertThat(context).getFailure()
                        .rootCause()
                        .hasMessageContaining("alerting.management")
                        .hasMessageContaining("must be set through ALERTING_MANAGEMENT_OPERATOR_PASSWORD"));
    }

    @Test
    void acceptsOperatorPasswordWhenRequired() {
        runner.withPropertyValues("alerting.management.operator-password-required=true",
                        "alerting.management.operator-password=test-only-password")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
