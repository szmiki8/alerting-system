/**
 * Relative paths of the Core's REST resources (architecture Section 9.1). All calls are
 * same-origin: Nginx in production and the dev-server proxy locally forward `/api` to the Core
 * (ADR-02), so no backend host appears in the UI.
 */
export const API_PATHS = {
  /** Returns 204 and sets the `XSRF-TOKEN` cookie; no side effects (OP-01, BE-13). */
  csrf: '/api/v1/csrf',
  emailSubscriptions: '/api/v1/subscriptions/email',
  slackSubscriptions: '/api/v1/subscriptions/slack',
  currentAdmin: '/api/v1/admin/me',
  subscribers: '/api/v1/admin/subscribers',
  subscriber: (id: string) => `/api/v1/admin/subscribers/${encodeURIComponent(id)}`,
} as const;

/** CSRF cookie and header names agreed with the Core (BE-13); these are Angular's defaults. */
export const XSRF_COOKIE_NAME = 'XSRF-TOKEN';
export const XSRF_HEADER_NAME = 'X-XSRF-TOKEN';
