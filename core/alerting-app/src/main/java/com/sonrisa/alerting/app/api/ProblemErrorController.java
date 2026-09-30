package com.sonrisa.alerting.app.api;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Answers the servlet container's error dispatch (errors raised outside Spring MVC, for example in a
 * filter) with Problem Details instead of Spring Boot's default error body. Replaces
 * {@code BasicErrorController}. Only the status is used; the exception and message are not sent.
 */
@Hidden
@RestController
class ProblemErrorController implements ErrorController {

    @RequestMapping("${server.error.path:${error.path:/error}}")
    ResponseEntity<ProblemDetail> error(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        // A direct request to the error path is not an error dispatch: treat it as an unknown route.
        HttpStatusCode status = code instanceof Integer value ? HttpStatusCode.valueOf(value) : HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(Problems.forStatus(status));
    }
}
