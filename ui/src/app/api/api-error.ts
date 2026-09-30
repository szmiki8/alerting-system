import { HttpErrorResponse } from '@angular/common/http';
import { MESSAGES } from '../core/messages';
import { FieldError, PROBLEM_TYPES, ProblemDetails } from './problem-details';

/**
 * What went wrong, as far as the UI needs to tell cases apart:
 * - `validation`: 400 problem; `fieldErrors` holds the invalid fields (FR-02).
 * - `unauthorized`: 401, no or expired admin session; the admin area reacts (FE-16, OP-03).
 * - `csrf`: 403 problem with the CSRF problem type; treated like a lost session (OP-03).
 * - `problem`: any other 4xx problem, for example 404 or the 422 "webhook not verified" (FR-04);
 *   callers decide by `status` or `type`.
 * - `server`: 5xx, with or without a problem body.
 * - `network`: no answer at all (status 0: offline, DNS, connection refused, aborted).
 * - `unexpected`: any other failed response, for example an HTML error page from a proxy.
 */
export type ApiErrorKind =
  'validation' | 'unauthorized' | 'csrf' | 'problem' | 'server' | 'network' | 'unexpected';

/**
 * The one error shape that API calls fail with (FE-08). `message` is always a user-friendly text
 * from the catalogue; `title`, `detail` and `type` are the Core's Problem Details for callers
 * that need a more specific text. Nothing from the request (for example a webhook URL) is kept.
 */
export class ApiError extends Error {
  override readonly name = 'ApiError';
  readonly kind: ApiErrorKind;
  /** HTTP status; 0 for network errors. */
  readonly status: number;
  readonly type: string | undefined;
  readonly title: string | undefined;
  readonly detail: string | undefined;
  readonly fieldErrors: readonly FieldError[];

  constructor(init: {
    kind: ApiErrorKind;
    status: number;
    message: string;
    type?: string;
    title?: string;
    detail?: string;
    fieldErrors?: readonly FieldError[];
  }) {
    super(init.message);
    this.kind = init.kind;
    this.status = init.status;
    this.type = init.type;
    this.title = init.title;
    this.detail = init.detail;
    this.fieldErrors = init.fieldErrors ?? [];
  }
}

/** Maps a failed HTTP response to an {@link ApiError}. Pure; used by the error interceptor. */
export function toApiError(response: HttpErrorResponse): ApiError {
  const status = response.status;
  if (status === 0) {
    return new ApiError({ kind: 'network', status, message: MESSAGES.apiError.network });
  }

  const problem = readProblem(response);
  const details = {
    status,
    type: problem?.type,
    title: problem?.title,
    detail: problem?.detail,
  };

  if (status === 401) {
    return new ApiError({
      ...details,
      kind: 'unauthorized',
      message: MESSAGES.apiError.sessionExpired,
    });
  }
  if (status === 403 && problem?.type === PROBLEM_TYPES.csrf) {
    return new ApiError({
      ...details,
      kind: 'csrf',
      message: MESSAGES.apiError.securityCheckFailed,
    });
  }
  if (status >= 500) {
    return new ApiError({ ...details, kind: 'server', message: MESSAGES.apiError.server });
  }
  if (problem && status === 400) {
    return new ApiError({
      ...details,
      kind: 'validation',
      message: MESSAGES.apiError.validation,
      fieldErrors: readFieldErrors(problem.errors),
    });
  }
  return new ApiError({
    ...details,
    kind: problem ? 'problem' : 'unexpected',
    message: MESSAGES.apiError.requestFailed,
  });
}

/**
 * Returns the Problem Details body, or `undefined` if the response has none. Accepts
 * `application/problem+json` and, as a fallback, a plain JSON object with a numeric `status`
 * (Spring may pick `application/json` depending on the request's Accept header).
 */
function readProblem(response: HttpErrorResponse): ProblemDetails | undefined {
  const body: unknown = response.error;
  if (typeof body !== 'object' || body === null || Array.isArray(body)) {
    return undefined;
  }
  const contentType = response.headers.get('Content-Type')?.toLowerCase() ?? '';
  const isProblem =
    contentType.startsWith('application/problem+json') ||
    (contentType.startsWith('application/json') &&
      typeof (body as ProblemDetails).status === 'number');
  if (!isProblem) {
    return undefined;
  }
  const { type, title, detail, errors } = body as Record<string, unknown>;
  return {
    type: asString(type),
    title: asString(title),
    detail: asString(detail),
    errors: Array.isArray(errors) ? errors : undefined,
  };
}

/** Keeps only well-formed `{ field, message }` entries. */
function readFieldErrors(errors: unknown[] | undefined): FieldError[] {
  return (errors ?? []).flatMap((entry) => {
    const { field, message } = (entry ?? {}) as Record<string, unknown>;
    return typeof field === 'string' && typeof message === 'string' ? [{ field, message }] : [];
  });
}

function asString(value: unknown): string | undefined {
  return typeof value === 'string' ? value : undefined;
}
