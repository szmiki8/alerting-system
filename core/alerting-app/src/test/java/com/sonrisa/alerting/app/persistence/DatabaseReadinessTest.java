package com.sonrisa.alerting.app.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Readiness turns DOWN when the database becomes unreachable (BE-06), so the load balancer stops routing to the
 * instance. Uses its own PostgreSQL container, because the test stops it.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.server.port=0",
        // Fail fast instead of waiting 30 seconds for a connection.
        "spring.datasource.hikari.connection-timeout=1000",
        "spring.datasource.hikari.validation-timeout=500"})
@ActiveProfiles("test")
@DirtiesContext
class DatabaseReadinessTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(PostgresTestcontainer.IMAGE);

    @LocalManagementPort
    int managementPort;

    @Test
    void readinessIsDownWhenTheDatabaseIsUnreachable() throws Exception {
        assertThat(readiness().statusCode()).isEqualTo(200);

        postgres.stop();

        HttpResponse<String> response = readiness();
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body()).contains("\"status\":\"DOWN\"");
    }

    private HttpResponse<String> readiness() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + managementPort + "/actuator/health/readiness")).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
