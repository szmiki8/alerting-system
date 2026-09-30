package com.sonrisa.alerting.app.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonrisa.alerting.app.AlertingApplication;
import com.sonrisa.alerting.app.architecture.fixture.AppClassUsingPlugin;
import com.sonrisa.alerting.app.architecture.fixture.ClientWithoutTimeouts;
import com.sonrisa.alerting.app.architecture.fixture.ControllerExposingEntity;
import com.sonrisa.alerting.app.architecture.fixture.ControllerUsingDto;
import com.sonrisa.alerting.archfixture.FixtureEntity;
import com.sonrisa.alerting.channel.archfixture.PluginUsingApp;
import com.sonrisa.alerting.source.archfixture.FakeSource;
import com.sonrisa.alerting.spi.archfixture.SpiUsingSpringBoot;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.Test;

/** Proves that each rule fails on a deliberately broken fixture (the fixtures are test code only). */
class ArchitectureRulesViolationTest {

    private static EvaluationResult evaluate(ArchRule rule, Class<?>... classes) {
        JavaClasses imported = new ClassFileImporter().importClasses(classes);
        return rule.evaluate(imported);
    }

    @Test
    void pluginImportingApplicationClassFails() {
        EvaluationResult result = evaluate(ArchitectureRules.PLUGINS_DO_NOT_DEPEND_ON_APP,
                PluginUsingApp.class, AlertingApplication.class);

        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().getDetails()).anyMatch(line -> line.contains("PluginUsingApp"));
    }

    @Test
    void applicationImportingPluginClassFails() {
        EvaluationResult result = evaluate(ArchitectureRules.APP_DOES_NOT_DEPEND_ON_PLUGINS,
                AppClassUsingPlugin.class, FakeSource.class);

        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().getDetails()).anyMatch(line -> line.contains("FakeSource"));
    }

    @Test
    void spiDependingOnSpringBootFails() {
        EvaluationResult result = evaluate(ArchitectureRules.SPI_HAS_NO_FRAMEWORK_DEPENDENCIES,
                SpiUsingSpringBoot.class);

        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().getDetails()).anyMatch(line -> line.contains("SpringApplication"));
    }

    @Test
    void controllerWithEntityInSignatureFails() {
        EvaluationResult result = evaluate(ArchitectureRules.CONTROLLERS_DO_NOT_EXPOSE_ENTITIES,
                ControllerExposingEntity.class, FixtureEntity.class);

        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().getDetails())
                .anyMatch(line -> line.contains("find(") && line.contains("FixtureEntity"))
                .anyMatch(line -> line.contains("save(") && line.contains("FixtureEntity"));
    }

    @Test
    void controllerWithDtoInSignaturePasses() {
        EvaluationResult result = evaluate(ArchitectureRules.CONTROLLERS_DO_NOT_EXPOSE_ENTITIES,
                ControllerUsingDto.class, FixtureEntity.class);

        assertThat(result.hasViolation()).isFalse();
    }

    @Test
    void applicationClassOutsideFeaturePackagesFails() {
        EvaluationResult result = evaluate(ArchitectureRules.APP_CLASSES_LIVE_IN_FEATURE_PACKAGES,
                AppClassUsingPlugin.class, AlertingApplication.class);

        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().getDetails())
                .anyMatch(line -> line.contains("AppClassUsingPlugin"))
                .noneMatch(line -> line.contains("AlertingApplication"));
    }

    @Test
    void httpClientWithoutTimeoutsFails() {
        EvaluationResult result = evaluate(ArchitectureRules.HTTP_CLIENTS_ARE_BUILT_WITH_TIMEOUTS,
                ClientWithoutTimeouts.class);

        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().getDetails())
                .anyMatch(line -> line.contains("RestClient.create()"))
                .anyMatch(line -> line.contains("RestClient.builder()"))
                .anyMatch(line -> line.contains("HttpClient.newHttpClient()"));
    }
}
