import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Observable } from 'rxjs';
import { ApiError } from './api-error';
import { apiErrorInterceptor } from './api-error.interceptor';
import { API_PATHS } from './api-paths';
import { PROBLEM_TYPES } from './problem-details';
import { SubscriptionApiService } from './subscription-api.service';

describe('SubscriptionApiService', () => {
  const webhookUrl = 'https://hooks.slack.com/services/T0001/B0001/abcDEF123';
  const problemJson = { 'Content-Type': 'application/problem+json' };
  const accepted = { status: 202, statusText: 'Accepted' };

  let service: SubscriptionApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(SubscriptionApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  /** Subscribes and records what the caller receives. */
  function record(call: Observable<void>) {
    const result: { next: unknown[]; completed: boolean; error?: unknown } = {
      next: [],
      completed: false,
    };
    call.subscribe({
      next: (value) => result.next.push(value),
      complete: () => (result.completed = true),
      error: (error: unknown) => (result.error = error),
    });
    return result;
  }

  describe('subscribeEmail', () => {
    it('POSTs name and email to the relative email resource', () => {
      record(service.subscribeEmail({ name: 'Ann', email: 'ann@example.com' }));
      const request = httpTesting.expectOne(API_PATHS.emailSubscriptions);
      expect(request.request.method).toBe('POST');
      expect(request.request.url).toBe('/api/v1/subscriptions/email');
      expect(request.request.body).toEqual({ name: 'Ann', email: 'ann@example.com' });
      request.flush({ message: 'Accepted' }, accepted);
    });

    it('maps 202 to a success without data (FR-05)', () => {
      const result = record(service.subscribeEmail({ name: 'Ann', email: 'ann@example.com' }));
      httpTesting
        .expectOne(API_PATHS.emailSubscriptions)
        .flush({ message: 'If the address is new, it has been subscribed.' }, accepted);
      expect(result).toEqual({ next: [undefined], completed: true });
    });

    it('maps a 400 validation problem to an ApiError with field errors', () => {
      const result = record(service.subscribeEmail({ name: 'Ann', email: 'ann@example' }));
      httpTesting.expectOne(API_PATHS.emailSubscriptions).flush(
        {
          type: 'urn:alerting:problem:validation',
          title: 'Invalid request',
          status: 400,
          errors: [{ field: 'email', message: 'must be a well-formed email address' }],
        },
        { status: 400, statusText: 'Bad Request', headers: problemJson },
      );
      expect(result.error).toBeInstanceOf(ApiError);
      expect(result.error).toMatchObject({
        kind: 'validation',
        status: 400,
        fieldErrors: [{ field: 'email', message: 'must be a well-formed email address' }],
      });
    });

    it('maps a network failure to an ApiError of kind network', () => {
      const result = record(service.subscribeEmail({ name: 'Ann', email: 'ann@example.com' }));
      httpTesting.expectOne(API_PATHS.emailSubscriptions).error(new ProgressEvent('error'));
      expect(result.error).toMatchObject({ kind: 'network', status: 0 });
    });
  });

  describe('subscribeSlack', () => {
    it('POSTs webhook URL and label to the relative Slack resource', () => {
      record(service.subscribeSlack({ webhookUrl, label: 'Newsroom' }));
      const request = httpTesting.expectOne(API_PATHS.slackSubscriptions);
      expect(request.request.method).toBe('POST');
      expect(request.request.url).toBe('/api/v1/subscriptions/slack');
      expect(request.request.body).toEqual({ webhookUrl, label: 'Newsroom' });
      request.flush({ message: 'Accepted' }, accepted);
    });

    it.each([undefined, '', '   '])('leaves out the label when it is %j', (label) => {
      record(service.subscribeSlack({ webhookUrl, label }));
      const request = httpTesting.expectOne(API_PATHS.slackSubscriptions);
      expect(request.request.body).toEqual({ webhookUrl });
      expect('label' in request.request.body).toBe(false);
      request.flush({ message: 'Accepted' }, accepted);
    });

    it('maps 202 to a success without data (FR-05)', () => {
      const result = record(service.subscribeSlack({ webhookUrl }));
      httpTesting.expectOne(API_PATHS.slackSubscriptions).flush({ message: 'Accepted' }, accepted);
      expect(result).toEqual({ next: [undefined], completed: true });
    });

    it('maps a 400 validation problem to an ApiError with field errors', () => {
      const result = record(service.subscribeSlack({ webhookUrl, label: 'x'.repeat(81) }));
      httpTesting.expectOne(API_PATHS.slackSubscriptions).flush(
        {
          type: 'urn:alerting:problem:validation',
          status: 400,
          errors: [{ field: 'label', message: 'size must be between 0 and 80' }],
        },
        { status: 400, statusText: 'Bad Request', headers: problemJson },
      );
      expect(result.error).toMatchObject({
        kind: 'validation',
        fieldErrors: [{ field: 'label', message: 'size must be between 0 and 80' }],
      });
    });

    it('maps the 422 verification failure to an ApiError with the webhook-not-verified type', () => {
      const result = record(service.subscribeSlack({ webhookUrl }));
      httpTesting.expectOne(API_PATHS.slackSubscriptions).flush(
        {
          type: PROBLEM_TYPES.webhookNotVerified,
          title: 'Webhook not verified',
          status: 422,
          detail: 'The welcome message could not be delivered.',
        },
        { status: 422, statusText: 'Unprocessable Content', headers: problemJson },
      );
      expect(result.error).toBeInstanceOf(ApiError);
      expect(result.error).toMatchObject({
        kind: 'problem',
        status: 422,
        type: PROBLEM_TYPES.webhookNotVerified,
        fieldErrors: [],
      });
      expect(JSON.stringify(result.error)).not.toContain(webhookUrl);
    });

    it('maps a network failure to an ApiError of kind network', () => {
      const result = record(service.subscribeSlack({ webhookUrl }));
      httpTesting.expectOne(API_PATHS.slackSubscriptions).error(new ProgressEvent('error'));
      expect(result.error).toMatchObject({ kind: 'network', status: 0 });
      expect((result.error as ApiError).message).not.toContain(webhookUrl);
    });
  });
});
