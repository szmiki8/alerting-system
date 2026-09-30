package com.sonrisa.alerting.app.api;

import java.util.List;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/** Creates the Problem Details bodies of the API. The only place that fills in type, title and detail. */
public final class Problems {

    /** Name of the extension member that lists the invalid fields of a validation problem. */
    public static final String ERRORS = "errors";

    private Problems() {
    }

    /** A problem of the given type with its fixed title and detail. */
    public static ProblemDetail of(ProblemType type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(type.status(), type.detail());
        problem.setType(type.uri());
        problem.setTitle(type.title());
        return problem;
    }

    /** A validation problem with one entry per invalid field. */
    public static ProblemDetail validation(List<FieldProblem> errors) {
        ProblemDetail problem = of(ProblemType.VALIDATION);
        problem.setProperty(ERRORS, List.copyOf(errors));
        return problem;
    }

    /**
     * The problem for an error that only has a status (framework exceptions, container errors). A status
     * without a type of its own gets {@code about:blank} with the status reason as title (RFC 9457).
     */
    public static ProblemDetail forStatus(HttpStatusCode status) {
        return ProblemType.forStatus(status)
                .map(Problems::of)
                .orElseGet(() -> ProblemDetail.forStatus(status));
    }

    /** Tells whether a body was created here (and can therefore be sent as it is). */
    static boolean isApiProblem(Object body) {
        return body instanceof ProblemDetail problem
                && problem.getType() != null
                && problem.getType().toString().startsWith(ProblemType.URI_PREFIX);
    }
}
