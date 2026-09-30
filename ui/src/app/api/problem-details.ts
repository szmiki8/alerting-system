/**
 * RFC 9457 Problem Details as returned by the Core with `application/problem+json`
 * (architecture Section 9.2, BE-12). All members are optional in the RFC, so the UI must not rely
 * on any of them being present.
 */
export interface ProblemDetails {
  /** Stable problem type identifier (see {@link PROBLEM_TYPES}); `about:blank` if generic. */
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  /** Field-level validation errors; present on 400 validation problems (BE-12). */
  errors?: FieldError[];
}

/** One invalid request field. `field` is the JSON property name of the request, e.g. `email`. */
export interface FieldError {
  field: string;
  message: string;
}

/**
 * Problem type identifiers the UI reacts to. They must match the Core's stable problem types
 * (BE-12 and BE-13, `ProblemType` in the Core's `api` package). Other types are handled by HTTP
 * status.
 */
export const PROBLEM_TYPES = {
  /** 403 when the CSRF token is missing or does not match (OP-03). */
  csrf: 'urn:alerting:problem:csrf-token-invalid',
  /** 422 when the Slack welcome message could not be delivered (FR-04, BE-19). */
  webhookNotVerified: 'urn:alerting:problem:webhook-not-verified',
} as const;
