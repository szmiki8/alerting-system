import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { TitleStrategy, provideRouter, withComponentInputBinding } from '@angular/router';
import { apiErrorInterceptor } from './api/api-error.interceptor';
import { XSRF_COOKIE_NAME, XSRF_HEADER_NAME } from './api/api-paths';
import { provideCsrfBootstrap } from './api/csrf-bootstrap';
import { routes } from './app.routes';
import { AppTitleStrategy } from './core/app-title-strategy';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Zoneless is the Angular 21 default; declared explicitly so the choice is visible (ADR-12).
    provideZonelessChangeDetection(),
    provideRouter(routes, withComponentInputBinding()),
    { provide: TitleStrategy, useClass: AppTitleStrategy },
    // Same-origin API client (ADR-02). Angular's XSRF support copies the CSRF cookie into the
    // header of POST, PUT, PATCH and DELETE requests to the own origin (NFR-05, OP-01).
    // Failed responses become one typed ApiError (FE-08).
    provideHttpClient(
      withXsrfConfiguration({ cookieName: XSRF_COOKIE_NAME, headerName: XSRF_HEADER_NAME }),
      withInterceptors([apiErrorInterceptor]),
    ),
    provideCsrfBootstrap(),
  ],
};
