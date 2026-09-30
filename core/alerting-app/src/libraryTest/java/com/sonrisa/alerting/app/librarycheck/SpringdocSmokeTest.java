package com.sonrisa.alerting.app.librarycheck;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** springdoc-openapi 3.x generates the contract under Spring Boot 4.1 and Swagger UI can be switched off per profile. */
class SpringdocSmokeTest {

    static HttpResponse<String> get(int port, String path) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Nested
    @SpringBootTest(classes = SmokeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    class SwaggerUiEnabled {

        @LocalServerPort
        int port;

        @Test
        void generatesOpenApiDocumentAndServesSwaggerUi() throws Exception {
            HttpResponse<String> apiDocs = get(port, "/v3/api-docs");
            assertThat(apiDocs.statusCode()).isEqualTo(200);
            assertThat(apiDocs.body()).contains("\"openapi\":\"3.1").contains("/api/v1/ping");

            assertThat(get(port, "/swagger-ui/index.html").statusCode()).isEqualTo(200);
        }
    }

    @Nested
    @SpringBootTest(classes = SmokeApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
            properties = "springdoc.swagger-ui.enabled=false")
    class SwaggerUiDisabled {

        @LocalServerPort
        int port;

        @Test
        void keepsApiDocsButHidesSwaggerUi() throws Exception {
            assertThat(get(port, "/v3/api-docs").statusCode()).isEqualTo(200);
            assertThat(get(port, "/swagger-ui/index.html").statusCode()).isEqualTo(404);
        }
    }
}
