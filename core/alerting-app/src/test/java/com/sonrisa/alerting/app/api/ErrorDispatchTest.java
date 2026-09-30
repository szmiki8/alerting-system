package com.sonrisa.alerting.app.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Errors raised outside Spring MVC reach the servlet container's error dispatch; they must also be
 * answered with Problem Details and no internals (BE-12). Needs a real server, MockMvc has no error
 * dispatch.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(ErrorDispatchTest.FailingFilter.class)
class ErrorDispatchTest {

    static final String FAILING_PATH = "/api/v1/test/filter-failure";
    static final String INTERNAL_MESSAGE = "filter internals 4711";

    @LocalServerPort
    int port;

    HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void errorInAFilterIsAProblemWithoutInternals() throws Exception {
        HttpResponse<String> response = get(FAILING_PATH);

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/problem+json");
        assertThat(response.body())
                .contains("\"type\":\"urn:alerting:problem:internal-error\"")
                .doesNotContain(INTERNAL_MESSAGE, "ServletException", "trace", "exception");
    }

    @Test
    void directCallOfTheErrorPathIsNotFound() throws Exception {
        HttpResponse<String> response = get("/error");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"type\":\"urn:alerting:problem:not-found\"");
    }

    @Test
    void unknownRouteOnTheRealServerIsNotFoundProblem() throws Exception {
        HttpResponse<String> response = get("/api/v1/does-not-exist");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/problem+json");
    }

    static class FailingFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            if (FAILING_PATH.equals(request.getRequestURI())) {
                throw new ServletException(INTERNAL_MESSAGE);
            }
            chain.doFilter(request, response);
        }
    }
}
