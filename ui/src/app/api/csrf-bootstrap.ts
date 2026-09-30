import { HttpClient } from '@angular/common/http';
import { EnvironmentProviders, inject, provideAppInitializer } from '@angular/core';
import { EMPTY, catchError } from 'rxjs';
import { API_PATHS } from './api-paths';

/**
 * Obtains the CSRF cookie once at start-up (OP-01). Nginx serves the UI, so the browser has no
 * `XSRF-TOKEN` cookie before its first call to the Core; `GET /api/v1/csrf` sets it, and Angular's
 * XSRF support then copies it into the `X-XSRF-TOKEN` header of every state-changing request.
 *
 * The call does not delay the first render: the page appears at once and the cookie arrives long
 * before anyone can fill in and submit a form. A failure is ignored here; the first POST then fails
 * with a 403 that the error handling reports.
 */
export function provideCsrfBootstrap(): EnvironmentProviders {
  return provideAppInitializer(() => {
    inject(HttpClient)
      .get<void>(API_PATHS.csrf)
      .pipe(catchError(() => EMPTY))
      .subscribe();
  });
}
