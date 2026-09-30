package com.sonrisa.alerting.app.api;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * The stable problem types of the {@code /api/v1} contract (BE-12, architecture Section 9.2). Clients
 * branch on {@link #uri()}; the {@link #title()} and {@link #detail()} are fixed texts so that error
 * bodies never carry request data (no echo of submitted values or secrets).
 *
 * <p>The identifiers are URNs: RFC 9457 allows any URI, and a URN makes clear that it is an identifier,
 * not a page to fetch. The list is part of the API contract and is published in the OpenAPI document.
 */
public enum ProblemType {

    VALIDATION("validation", HttpStatus.BAD_REQUEST, "Validation failed",
            "One or more fields are invalid."),
    BAD_REQUEST("bad-request", HttpStatus.BAD_REQUEST, "Bad request",
            "The request is malformed."),
    WEBHOOK_NOT_VERIFIED("webhook-not-verified", HttpStatus.BAD_REQUEST, "Webhook not verified",
            "The welcome message could not be delivered to the webhook."),
    UNAUTHENTICATED("unauthenticated", HttpStatus.UNAUTHORIZED, "Authentication required",
            "Sign in to access this resource."),
    ACCESS_DENIED("access-denied", HttpStatus.FORBIDDEN, "Access denied",
            "You are not allowed to access this resource."),
    CSRF_TOKEN_INVALID("csrf-token-invalid", HttpStatus.FORBIDDEN, "CSRF token missing or invalid",
            "Reload the page and try again."),
    NOT_FOUND("not-found", HttpStatus.NOT_FOUND, "Not found",
            "The requested resource does not exist."),
    METHOD_NOT_ALLOWED("method-not-allowed", HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed",
            "The resource does not support this HTTP method."),
    NOT_ACCEPTABLE("not-acceptable", HttpStatus.NOT_ACCEPTABLE, "Not acceptable",
            "The API responds with JSON only."),
    UNSUPPORTED_MEDIA_TYPE("unsupported-media-type", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type",
            "The API accepts JSON only."),
    INTERNAL_ERROR("internal-error", HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
            "An unexpected error occurred.");

    /** Prefix of every problem type identifier. */
    public static final String URI_PREFIX = "urn:alerting:problem:";

    private final URI uri;
    private final HttpStatus status;
    private final String title;
    private final String detail;

    ProblemType(String slug, HttpStatus status, String title, String detail) {
        this.uri = URI.create(URI_PREFIX + slug);
        this.status = status;
        this.title = title;
        this.detail = detail;
    }

    public URI uri() {
        return uri;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    public String detail() {
        return detail;
    }

    /**
     * The generic type for a status, used for errors raised by the framework (unknown route, wrong
     * method, unreadable body, ...). Types that share a status with another (validation, webhook not
     * verified, CSRF) are only used where they are raised explicitly.
     */
    static Optional<ProblemType> forStatus(HttpStatusCode status) {
        return Arrays.stream(values())
                .filter(type -> type.status.value() == status.value())
                .filter(type -> type != VALIDATION && type != WEBHOOK_NOT_VERIFIED && type != CSRF_TOKEN_INVALID)
                .findFirst();
    }
}
