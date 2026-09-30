package com.sonrisa.alerting.app.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonrisa.alerting.app.config.DeployedProfileArguments;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Base64;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.ActiveProfiles;

/**
 * The whole application starts on H2 (no external database) and, with the {@code postgres} profile, on
 * PostgreSQL; only configuration differs (BE-06). In both cases Flyway has migrated the schema at start-up,
 * Hibernate only validates it, and the readiness group contains the database check.
 */
class DatabaseStartupTest {

    static final String OPERATOR_PASSWORD = "test-only-operator-password";

    abstract static class StartupChecks {

        @Autowired
        Flyway flyway;

        @Autowired
        DataSource dataSource;

        @Value("${spring.jpa.hibernate.ddl-auto}")
        String ddlAuto;

        @LocalManagementPort
        int managementPort;

        abstract String expectedDatabaseProduct();

        @Test
        void usesTheExpectedDatabase() throws Exception {
            try (Connection connection = dataSource.getConnection()) {
                assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo(expectedDatabaseProduct());
            }
        }

        @Test
        void flywayMigratedAtStartup() {
            assertThat(flyway.info().pending()).isEmpty();
            assertThat(flyway.info().applied()).isNotEmpty();
        }

        @Test
        void hibernateOnlyValidatesTheSchema() {
            assertThat(ddlAuto).isEqualTo("validate");
        }

        @Test
        void readinessIncludesTheDatabase() throws Exception {
            String credentials = Base64.getEncoder()
                    .encodeToString(("operator:" + OPERATOR_PASSWORD).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create("http://localhost:" + managementPort + "/actuator/health/readiness"))
                    .header("Authorization", "Basic " + credentials)
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"db\"").contains("\"status\":\"UP\"");
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
            "management.server.port=0",
            "alerting.management.operator-password=" + OPERATOR_PASSWORD})
    @ActiveProfiles("test")
    class OnH2 extends StartupChecks {

        @Override
        String expectedDatabaseProduct() {
            return "H2";
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
            "management.server.port=0",
            "alerting.management.operator-password=" + OPERATOR_PASSWORD,
            // The postgres profile requires the keys (BE-09); test-only values.
            "alerting.security.encryption-key=" + DeployedProfileArguments.TEST_ENCRYPTION_KEY,
            "alerting.security.fingerprint-key=" + DeployedProfileArguments.TEST_FINGERPRINT_KEY,
            // Plain logs in tests; the profile's JSON format would stay active for later tests in this JVM.
            "logging.structured.format.console="})
    @ActiveProfiles("postgres")
    @ImportTestcontainers(PostgresTestcontainer.class)
    class OnPostgresProfile extends StartupChecks {

        @Override
        String expectedDatabaseProduct() {
            return "PostgreSQL";
        }
    }
}
