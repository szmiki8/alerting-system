package com.sonrisa.alerting.app.api;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error raised while handling an API request into Problem Details (RFC 9457).
 *
 * <p>Spring MVC's own exceptions (unknown route, wrong method, unreadable body, type mismatch, ...) keep
 * their status, but their body is replaced by the fixed problem of that status: Spring's default details
 * can quote request data (for example "Failed to convert value 'x'"), and error bodies must never echo
 * submitted values (Section 9.2). Validation errors list the invalid fields with their messages only.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Replaces messages of type conversion failures, which quote the rejected value. */
    static final String INVALID_VALUE_MESSAGE = "has an invalid value";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, Problems.validation(fieldProblems(ex.getBindingResult())), headers,
                ProblemType.VALIDATION.status(), request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldProblem> errors = new ArrayList<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                errors.addAll(fieldProblems(parameterErrors));
            } else {
                String parameter = result.getMethodParameter().getParameterName();
                for (MessageSourceResolvable error : result.getResolvableErrors()) {
                    errors.add(new FieldProblem(parameter, error.getDefaultMessage()));
                }
            }
        }
        return handleExceptionInternal(ex, Problems.validation(errors), headers, ProblemType.VALIDATION.status(),
                request);
    }

    /** Anything not handled elsewhere: 500 without any internal details. The exception goes to the log. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error while handling an API request", ex);
        return handleExceptionInternal(ex, Problems.of(ProblemType.INTERNAL_ERROR), new HttpHeaders(),
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        // Spring passes no body for its own exceptions; a ProblemException carries its problem as ErrorResponse.
        Object candidate = body == null && ex instanceof ErrorResponse errorResponse ? errorResponse.getBody() : body;
        Object problem = Problems.isApiProblem(candidate) ? candidate : Problems.forStatus(statusCode);
        return super.handleExceptionInternal(ex, problem, headers, statusCode, request);
    }

    private static List<FieldProblem> fieldProblems(Errors errors) {
        List<FieldProblem> problems = new ArrayList<>();
        for (FieldError error : errors.getFieldErrors()) {
            String message = error.isBindingFailure() ? INVALID_VALUE_MESSAGE : error.getDefaultMessage();
            problems.add(new FieldProblem(error.getField(), message));
        }
        // Class-level constraints have no field; they are reported without a field name.
        errors.getGlobalErrors().forEach(error -> problems.add(new FieldProblem(null, error.getDefaultMessage())));
        return problems;
    }
}
