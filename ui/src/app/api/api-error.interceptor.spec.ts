import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { MESSAGES } from '../core/messages';
import { ApiError } from './api-error';
import { apiErrorInterceptor } from './api-error.interceptor';
import { API_PATHS } from './api-paths';
import { PROBLEM_TYPES } from './problem-details';

describe('apiErrorInterceptor', () => {
  const url = API_PATHS.slackSubscriptions;
  const webhookUrl = 'https://hooks.slack.com/services/T000/B000/secret-part';
  const problemJson = { 'Content-Type': 'application/problem+json' };

  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  /** Sends a POST with a secret in the body and returns the error the caller receives. */
  function failWith(
    body: string | object | null,
    status: number,
    headers: Record<string, string> = {},
  ): unknown {
    let received: unknown;
    http
      .post(url, { webhookUrl, label: 'news' })
      .subscribe({ error: (e: unknown) => (received = e) });
    httpTesting.expectOne(url).flush(body, { status, statusText: 'Error', headers });
    return received;
  }

  function expectNoSecret(error: unknown): void {
    const apiError = error as ApiError;
    expect(apiError.message).not.toContain(webhookUrl);
    expect(JSON.stringify({ ...apiError, message: apiError.message })).not.toContain('secret-part');
  }

  it('maps a 400 problem to a validation error with field errors', () => {
    const error = failWith(
      {
        type: 'urn:alerting:problem:validation',
        title: 'Invalid request',
        status: 400,
        detail: 'The request has invalid fields.',
        errors: [
          { field: 'webhookUrl', message: 'must be a Slack incoming webhook URL' },
          { field: 'label', message: 'size must be at most 80' },
          { field: 42, message: 'malformed entries are ignored' },
        ],
      },
      400,
      problemJson,
    );
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      kind: 'validation',
      status: 400,
      type: 'urn:alerting:problem:validation',
      title: 'Invalid request',
      detail: 'The request has invalid fields.',
      message: MESSAGES.apiError.validation,
      fieldErrors: [
        { field: 'webhookUrl', message: 'must be a Slack incoming webhook URL' },
        { field: 'label', message: 'size must be at most 80' },
      ],
    });
    expectNoSecret(error);
  });

  it('maps a 422 problem to a problem error that keeps type, title and detail', () => {
    const error = failWith(
      {
        type: PROBLEM_TYPES.webhookNotVerified,
        title: 'Webhook not verified',
        status: 422,
        detail: 'The welcome message could not be delivered.',
      },
      422,
      { 'Content-Type': 'application/problem+json;charset=UTF-8' },
    );
    expect(error).toMatchObject({
      kind: 'problem',
      status: 422,
      type: PROBLEM_TYPES.webhookNotVerified,
      title: 'Webhook not verified',
      detail: 'The welcome message could not be delivered.',
      message: MESSAGES.apiError.requestFailed,
      fieldErrors: [],
    });
    expectNoSecret(error);
  });

  it('accepts a problem body sent as application/json', () => {
    const error = failWith({ title: 'Not Found', status: 404 }, 404, {
      'Content-Type': 'application/json',
    });
    expect(error).toMatchObject({ kind: 'problem', status: 404, title: 'Not Found' });
  });

  it('passes 401 through as its own kind and does not navigate', () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate');
    const navigateByUrl = vi.spyOn(router, 'navigateByUrl');
    const error = failWith({ title: 'Unauthorized', status: 401 }, 401, problemJson);
    expect(error).toMatchObject({
      kind: 'unauthorized',
      status: 401,
      message: MESSAGES.apiError.sessionExpired,
    });
    expect(navigate).not.toHaveBeenCalled();
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('maps a plain 401 without body to unauthorized', () => {
    expect(failWith(null, 401)).toMatchObject({ kind: 'unauthorized', status: 401 });
  });

  it('maps a 403 problem with the CSRF type to the csrf kind', () => {
    const error = failWith({ type: PROBLEM_TYPES.csrf, status: 403 }, 403, problemJson);
    expect(error).toMatchObject({
      kind: 'csrf',
      status: 403,
      message: MESSAGES.apiError.securityCheckFailed,
    });
  });

  it('maps any other 403 problem to the problem kind', () => {
    const error = failWith(
      { type: 'urn:alerting:problem:access-denied', status: 403 },
      403,
      problemJson,
    );
    expect(error).toMatchObject({ kind: 'problem', status: 403 });
  });

  it('maps a 5xx without a problem body to the generic server error', () => {
    const error = failWith('<html>Bad Gateway</html>', 502, { 'Content-Type': 'text/html' });
    expect(error).toMatchObject({
      kind: 'server',
      status: 502,
      message: MESSAGES.apiError.server,
      title: undefined,
    });
  });

  it('maps a 5xx problem to the generic server error but keeps its details', () => {
    const error = failWith({ title: 'Internal Server Error', status: 500 }, 500, problemJson);
    expect(error).toMatchObject({
      kind: 'server',
      message: MESSAGES.apiError.server,
      title: 'Internal Server Error',
    });
  });

  it('maps a network failure (status 0) to the network kind', () => {
    let received: unknown;
    http.post(url, { webhookUrl }).subscribe({ error: (e: unknown) => (received = e) });
    httpTesting.expectOne(url).error(new ProgressEvent('error'));
    expect(received).toMatchObject({
      kind: 'network',
      status: 0,
      message: MESSAGES.apiError.network,
    });
    expectNoSecret(received);
  });

  it('maps a 4xx response without a problem body to the unexpected kind', () => {
    const error = failWith('<html>Not Found</html>', 404, { 'Content-Type': 'text/html' });
    expect(error).toMatchObject({
      kind: 'unexpected',
      status: 404,
      message: MESSAGES.apiError.requestFailed,
    });
  });

  it('leaves successful responses untouched', () => {
    let body: unknown;
    http.get(API_PATHS.currentAdmin).subscribe((b) => (body = b));
    httpTesting.expectOne(API_PATHS.currentAdmin).flush({ email: 'a@example.com', name: 'A' });
    expect(body).toEqual({ email: 'a@example.com', name: 'A' });
  });
});
