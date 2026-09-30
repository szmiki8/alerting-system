package com.sonrisa.alerting.app.api;

import java.util.List;
import org.springframework.http.ProblemDetail;
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
        this(type, Problems.of(type), cause);
    }

    private ProblemException(ProblemType type, ProblemDetail body, Throwable cause) {
        super(type.status(), body, cause);
        this.type = type;
    }

    /**
     * A {@link ProblemType#VALIDATION} problem with the given invalid fields, for checks that run in application
     * code rather than through {@code @Valid} (for example a subscriber type's own validation).
     */
    public static ProblemException validation(List<FieldProblem> errors) {
        return new ProblemException(ProblemType.VALIDATION, Problems.validation(errors), null);
    }

    public ProblemType type() {
        return type;
    }
}
