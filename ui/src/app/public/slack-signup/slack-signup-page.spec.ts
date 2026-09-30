import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, Subject } from 'rxjs';
import { ApiError } from '../../api/api-error';
import { PROBLEM_TYPES } from '../../api/problem-details';
import { SubscriptionApiService } from '../../api/subscription-api.service';
import { SlackSubscriptionRequest } from '../../api/subscription.models';
import { MESSAGES } from '../../core/messages';
import { SlackSignupPage } from './slack-signup-page';

describe('SlackSignupPage', () => {
  const webhookUrl =
    'https://hooks.slack.com/services/T0001ABCDE/B0002DEFGH/abcDEF123xyz456GHI789jk';
  const secretPart = 'abcDEF123xyz';

  let fixture: ComponentFixture<SlackSignupPage>;
  let el: HTMLElement;
  let requests: SlackSubscriptionRequest[];
  let response: Subject<void>;
  let consoleCalls: unknown[][];

  beforeEach(async () => {
    requests = [];
    consoleCalls = [];
    for (const method of ['log', 'info', 'warn', 'error', 'debug'] as const) {
      vi.spyOn(console, method).mockImplementation((...args: unknown[]) => {
        consoleCalls.push(args);
      });
    }
    const api: Pick<SubscriptionApiService, 'subscribeSlack'> = {
      subscribeSlack: (request): Observable<void> => {
        requests.push(request);
        response = new Subject<void>();
        return response;
      },
    };
    TestBed.configureTestingModule({
      providers: [{ provide: SubscriptionApiService, useValue: api }],
    });
    fixture = TestBed.createComponent(SlackSignupPage);
    el = fixture.nativeElement;
    document.body.appendChild(el); // focus needs attached elements
    await fixture.whenStable();
  });

  afterEach(() => {
    // The webhook URL is a secret: it must never reach the console (NFR-04).
    expect(JSON.stringify(consoleCalls)).not.toContain(secretPart);
    vi.restoreAllMocks();
    document.body.replaceChildren();
  });

  const urlInput = () => el.querySelector<HTMLInputElement>('#slack-signup-webhook-url')!;
  const labelInput = () => el.querySelector<HTMLInputElement>('#slack-signup-label')!;
  const button = () => el.querySelector<HTMLButtonElement>('button[type="submit"]')!;
  const politeText = () =>
    el.querySelector('app-status-message [role="status"]')!.textContent!.trim();
  const alertText = () =>
    el.querySelector('app-status-message [role="alert"]')!.textContent!.trim();
  const summaryLinks = () =>
    Array.from(el.querySelectorAll('.error-summary a')).map((a) => a.textContent!.trim());
  /** All text a visitor can see or hear outside the form fields. */
  const visibleText = () => el.textContent ?? '';

  async function type(input: HTMLInputElement, value: string): Promise<void> {
    input.value = value;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  }

  async function submit(): Promise<void> {
    button().click();
    await fixture.whenStable();
  }

  async function failWith(error: unknown): Promise<void> {
    await type(urlInput(), webhookUrl);
    await submit();
    response.error(error);
    await fixture.whenStable();
  }

  describe('form fields', () => {
    it('has a required webhook URL field that browsers do not autofill or save', () => {
      const label = el.querySelector(`label[for="${urlInput().id}"]`)?.textContent;
      expect(label?.replace('*', '').trim()).toBe('Slack webhook URL');
      expect(urlInput().getAttribute('aria-required')).toBe('true');
      expect(urlInput().type).toBe('url');
      expect(urlInput().getAttribute('autocomplete')).toBe('off');
    });

    it('has an optional label field', () => {
      const label = el.querySelector(`label[for="${labelInput().id}"]`)?.textContent?.trim();
      expect(label).toBe('Label (optional)');
      expect(labelInput().getAttribute('aria-required')).toBe('false');
    });

    it('links to the Slack documentation in the same tab, marked as external', () => {
      const link = Array.from(el.querySelectorAll('a')).find((a) =>
        a.href.startsWith('https://docs.slack.dev/'),
      )!;
      expect(link.textContent).toContain('(external site)');
      expect(link.hasAttribute('target')).toBe(false);
    });
  });

  describe('client validation', () => {
    it('requires the webhook URL and sends nothing', async () => {
      await submit();
      expect(requests).toEqual([]);
      expect(summaryLinks()).toEqual(['Slack webhook URL is required.']);
      expect(document.activeElement).toBe(el.querySelector('.error-summary'));
    });

    it.each([
      'https://example.com/services/T0001/B0001/abc',
      'http://hooks.slack.com/services/T0001/B0001/abc',
      'https://hooks.slack.com.evil.example/services/T0001/B0001/abc',
      'https://hooks.slack.com/services/T0001/B0001',
      'https://hooks.slack.com/services/t0001/B0001/abc',
      'https://hooks.slack.com/services/T0001/B0001/abc?x=1',
      'https://hooks.slack.com/services/T0001ABCDE/B0002DEFGH/tooShortSecret',
    ])('rejects %s and sends nothing', async (url) => {
      await type(urlInput(), url);
      await submit();
      expect(requests).toEqual([]);
      expect(summaryLinks()).toEqual([MESSAGES.signup.webhookUrlFormat]);
      expect(urlInput().getAttribute('aria-invalid')).toBe('true');
      expect(urlInput().value).toBe(url);
    });

    it('accepts a pasted URL with surrounding spaces (the url input strips them)', async () => {
      await type(urlInput(), `  ${webhookUrl}\n`);
      await submit();
      expect(requests).toEqual([{ webhookUrl, label: '' }]);
    });

    it('rejects a label longer than 100 characters', async () => {
      await type(urlInput(), webhookUrl);
      await type(labelInput(), 'x'.repeat(101));
      await submit();
      expect(requests).toEqual([]);
      expect(summaryLinks()).toEqual(['Label must be 100 characters or fewer.']);
    });
  });

  describe('successful submit', () => {
    it('sends the webhook URL and the label', async () => {
      await type(urlInput(), webhookUrl);
      await type(labelInput(), 'Newsroom');
      await submit();
      expect(requests).toEqual([{ webhookUrl, label: 'Newsroom' }]);
    });

    it('shows an announced busy state while the webhook is verified', async () => {
      await type(urlInput(), webhookUrl);
      await submit();
      expect(button().getAttribute('aria-disabled')).toBe('true');
      expect(el.querySelector('app-submit-button [role="status"]')?.textContent?.trim()).toBe(
        MESSAGES.signup.verifyingWebhook,
      );
      await submit();
      expect(requests.length).toBe(1);
    });

    it('announces the generic confirmation and clears the form, including the URL (FR-05)', async () => {
      await type(urlInput(), webhookUrl);
      await type(labelInput(), 'Newsroom');
      await submit();
      response.next();
      response.complete();
      await fixture.whenStable();

      expect(politeText()).toBe(MESSAGES.signup.slackConfirmation);
      expect(urlInput().value).toBe('');
      expect(labelInput().value).toBe('');
      expect(el.querySelector('mat-error')).toBeNull();
      expect(button().hasAttribute('aria-disabled')).toBe(false);
      expect(fixture.componentInstance['form'].getRawValue()).toEqual({
        webhookUrl: '',
        label: '',
      });
    });
  });

  describe('failed submit', () => {
    it('shows verification guidance for a 422, keeps the URL and does not show it', async () => {
      await failWith(
        new ApiError({
          kind: 'problem',
          status: 422,
          type: PROBLEM_TYPES.webhookNotVerified,
          title: 'Webhook not verified',
          message: MESSAGES.apiError.requestFailed,
        }),
      );
      expect(alertText()).toBe(MESSAGES.signup.webhookNotVerified);
      expect(alertText()).toContain('could not be verified');
      expect(politeText()).toBe('');
      expect(urlInput().value).toBe(webhookUrl);
      expect(visibleText()).not.toContain(secretPart);
      expect(button().hasAttribute('aria-disabled')).toBe(false);
    });

    it('shows server field errors on the matching fields', async () => {
      await failWith(
        new ApiError({
          kind: 'validation',
          status: 400,
          message: MESSAGES.apiError.validation,
          fieldErrors: [{ field: 'webhookUrl', message: 'Not a Slack incoming webhook URL.' }],
        }),
      );
      expect(summaryLinks()).toEqual(['Not a Slack incoming webhook URL.']);
      expect(document.activeElement).toBe(el.querySelector('.error-summary'));
      expect(urlInput().getAttribute('aria-invalid')).toBe('true');
      expect(urlInput().value).toBe(webhookUrl);
    });

    it('shows a general message when no server field error matches a field', async () => {
      await failWith(
        new ApiError({ kind: 'validation', status: 400, message: MESSAGES.apiError.validation }),
      );
      expect(alertText()).toBe(MESSAGES.apiError.validation);
    });

    it('shows a general message for a network error and keeps the URL', async () => {
      await failWith(
        new ApiError({ kind: 'network', status: 0, message: MESSAGES.apiError.network }),
      );
      expect(alertText()).toBe(MESSAGES.apiError.network);
      expect(urlInput().value).toBe(webhookUrl);
      expect(visibleText()).not.toContain(secretPart);
    });

    it('shows the CSRF message for a failed security check', async () => {
      await failWith(
        new ApiError({
          kind: 'csrf',
          status: 403,
          type: PROBLEM_TYPES.csrf,
          message: MESSAGES.apiError.securityCheckFailed,
        }),
      );
      expect(alertText()).toBe(MESSAGES.apiError.securityCheckFailed);
    });

    it('shows a generic message for an error that is not an ApiError', async () => {
      await failWith(new Error(`failed for ${webhookUrl}`));
      expect(alertText()).toBe(MESSAGES.apiError.requestFailed);
      expect(visibleText()).not.toContain(secretPart);
    });
  });
});
