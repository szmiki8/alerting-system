import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { toApiError } from './api-error';

/**
 * Turns every failed HTTP response into an {@link ApiError} (FE-08), so services and components
 * never parse HTTP details themselves. It only maps: it does not navigate, retry or show messages;
 * the admin area reacts to the `unauthorized` kind itself (FE-16).
 */
export const apiErrorInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(
    catchError((error: unknown) =>
      throwError(() => (error instanceof HttpErrorResponse ? toApiError(error) : error)),
    ),
  );
