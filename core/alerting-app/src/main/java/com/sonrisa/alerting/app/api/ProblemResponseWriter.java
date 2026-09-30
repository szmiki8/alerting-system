package com.sonrisa.alerting.app.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes a problem directly to the servlet response, for code that runs before Spring MVC (the security
 * filters: 401 for a missing login, 403 for a missing CSRF token). The body is the same as the one the
 * MVC error handling produces, including {@code instance} (the request path).
 */
@Component
public class ProblemResponseWriter {

    private final JsonMapper jsonMapper;

    public ProblemResponseWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, ProblemType type) throws IOException {
        ProblemDetail problem = Problems.of(type);
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(type.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
