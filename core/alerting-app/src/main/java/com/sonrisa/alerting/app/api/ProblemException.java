package com.sonrisa.alerting.app.api;

import org.springframework.web.ErrorResponseException;

/**
 * Thrown by application code to answer with a problem of a given type, for example
 * {@code throw new ProblemException(ProblemType.NOT_FOUND)}. The cause is for the logs only; the client
 * gets the fixed title and detail of the type.
 */
public class ProblemException extends ErrorResponseException {

    private final ProblemType type;

    public ProblemException(ProblemType type) {
        this(type, null);
    }

    public ProblemException(ProblemType type, Throwable cause) {
        super(type.status(), Problems.of(type), cause);
        this.type = type;
    }

    public ProblemType type() {
        return type;
    }
}
