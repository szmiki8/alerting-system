package com.sonrisa.alerting.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

class ManagementEndpointsTest {

    static final String OPERATOR = "operator";
    static final String TEST_PASSWORD = "test-only-operator-password";

    static HttpResponse<String> get(int port, String path, String user, String password) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (user != null) {
            String token = Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
            request.header("Authorization", "Basic " + token);
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    static HttpResponse<String> get(int port, String path) throws Exception {
        return get(port, path, null, null);
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
            "management.server.port=0",
            "alerting.management.operator-password=" + TEST_PASSWORD})
    @ActiveProfiles("test")
    class WithOperatorCredential {

        @LocalServerPort
        int serverPort;

        @LocalManagementPort
        int managementPort;

        @Value("${server.shutdown}")
        String shutdown;

        @Test
        void managementRunsOnItsOwnPort() {
            assertThat(managementPort).isPositive().isNotEqualTo(serverPort);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/actuator", "/actuator/health", "/actuator/health/liveness", "/actuator/info",
                "/actuator/prometheus"})
        void applicationPortDoesNotServeActuator(String path) throws Exception {
            assertThat(get(serverPort, path).statusCode()).isEqualTo(404);
            assertThat(get(serverPort, path, OPERATOR, TEST_PASSWORD).statusCode()).isEqualTo(404);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/actuator/health/liveness", "/actuator/health/readiness"})
        void probesAnswerWithoutCredentials(String path) throws Exception {
            HttpResponse<String> response = get(managementPort, path);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"status\":\"UP\"");
        }

        @Test
        void healthShowsNoDetailsWithoutCredentials() throws Exception {
            HttpResponse<String> response = get(managementPort, "/actuator/health");

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).doesNotContain("components");
        }

        @ParameterizedTest
        @ValueSource(strings = {"/actuator/info", "/actuator/prometheus"})
        void protectedEndpointsNeedTheOperatorCredential(String path) throws Exception {
            assertThat(get(managementPort, path).statusCode()).isEqualTo(401);
            assertThat(get(managementPort, path, OPERATOR, "wrong-password").statusCode()).isEqualTo(401);
            assertThat(get(managementPort, path, "someone-else", TEST_PASSWORD).statusCode()).isEqualTo(401);
            assertThat(get(managementPort, path, OPERATOR, TEST_PASSWORD).statusCode()).isEqualTo(200);
        }

        @Test
        void prometheusExportsMetrics() throws Exception {
            HttpResponse<String> response = get(managementPort, "/actuator/prometheus", OPERATOR, TEST_PASSWORD);

            assertThat(response.body()).contains("jvm_memory_used_bytes");
        }

        @Test
        void gracefulShutdownIsEnabled() {
            assertThat(shutdown).isEqualTo("graceful");
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "management.server.port=0")
    @ActiveProfiles("test")
    class WithoutOperatorCredential {

        @LocalManagementPort
        int managementPort;

        @Test
        void onlyHealthIsReachable() throws Exception {
            assertThat(get(managementPort, "/actuator/health/readiness").statusCode()).isEqualTo(200);
            assertThat(get(managementPort, "/actuator/info").statusCode()).isEqualTo(401);
            assertThat(get(managementPort, "/actuator/info", OPERATOR, "").statusCode()).isEqualTo(401);
        }
    }
}
