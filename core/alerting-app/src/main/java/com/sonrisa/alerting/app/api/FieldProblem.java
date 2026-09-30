package com.sonrisa.alerting.app.api;

/**
 * One invalid field in a {@link ProblemType#VALIDATION} problem ({@code errors} list).
 *
 * @param field the field name as the client sent it (nested fields with dots, e.g. {@code address.city})
 * @param message a human-readable message; never contains the submitted value
 */
public record FieldProblem(String field, String message) {
}
