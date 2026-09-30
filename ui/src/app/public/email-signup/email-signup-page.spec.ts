import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, Subject } from 'rxjs';
import { ApiError } from '../../api/api-error';
import { SubscriptionApiService } from '../../api/subscription-api.service';
import { EmailSubscriptionRequest } from '../../api/subscription.models';
import { MESSAGES } from '../../core/messages';
import { EmailSignupPage } from './email-signup-page';

describe('EmailSignupPage', () => {
  let fixture: ComponentFixture<EmailSignupPage>;
  let el: HTMLElement;
  let requests: EmailSubscriptionRequest[];
  let response: Subject<void>;

  beforeEach(async () => {
    requests = [];
    const api: Pick<SubscriptionApiService, 'subscribeEmail'> = {
      subscribeEmail: (request): Observable<void> => {
        requests.push(request);
        response = new Subject<void>();
        return response;
      },
    };
    TestBed.configureTestingModule({
      providers: [{ provide: SubscriptionApiService, useValue: api }],
    });
    fixture = TestBed.createComponent(EmailSignupPage);
    el = fixture.nativeElement;
    document.body.appendChild(el); // focus needs attached elements
    await fixture.whenStable();
  });

  afterEach(() => document.body.replaceChildren());

  const nameInput = () => el.querySelector<HTMLInputElement>('#email-signup-name')!;
  const emailInput = () => el.querySelector<HTMLInputElement>('#email-signup-email')!;
  const button = () => el.querySelector<HTMLButtonElement>('button[type="submit"]')!;
  const politeText = () =>
    el.querySelector('app-status-message [role="status"]')!.textContent!.trim();
  const alertText = () =>
    el.querySelector('app-status-message [role="alert"]')!.textContent!.trim();
  const summaryLinks = () =>
    Array.from(el.querySelectorAll('.error-summary a')).map((a) => a.textContent!.trim());
  const errorOf = (input: HTMLInputElement) => {
    const ids = input.getAttribute('aria-describedby')?.split(' ') ?? [];
    return ids
      .map((id) => document.getElementById(id))
      .find((node) => node?.tagName.toLowerCase() === 'mat-error')
      ?.textContent?.trim();
  };

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

  async function fillValid(): Promise<void> {
    await type(nameInput(), 'Ann Example');
    await type(emailInput(), 'ann@example.com');
  }

  describe('form fields', () => {
    it('has labelled, required name and email fields with autocomplete hints', () => {
      const labelOf = (input: HTMLInputElement) =>
        el.querySelector(`label[for="${input.id}"]`)?.textContent?.replace('*', '').trim();
      expect(labelOf(nameInput())).toBe('Name');
      expect(labelOf(emailInput())).toBe('Email');
      expect(nameInput().getAttribute('autocomplete')).toBe('name');
      expect(emailInput().getAttribute('autocomplete')).toBe('email');
      expect(emailInput().type).toBe('email');
      expect(nameInput().getAttribute('aria-required')).toBe('true');
      expect(emailInput().getAttribute('aria-required')).toBe('true');
    });

    it('states what the visitor will receive', () => {
      expect(el.querySelector('.signup-intro')?.textContent).toContain(
        'one email for each new event',
      );
    });
  });

  describe('client validation', () => {
    it('rejects empty input with a message per field, focuses the summary and sends nothing', async () => {
      await submit();
      expect(requests).toEqual([]);
      expect(summaryLinks()).toEqual(['Name is required.', 'Email is required.']);
      expect(document.activeElement).toBe(el.querySelector('.error-summary'));
      expect(errorOf(nameInput())).toBe('Name is required.');
      expect(errorOf(emailInput())).toBe('Email is required.');
      // Material leaves aria-invalid off for empty required fields; the linked error is read.
    });

    it('rejects a name of only spaces', async () => {
      await type(nameInput(), '   ');
      await type(emailInput(), 'ann@example.com');
      await submit();
      expect(requests).toEqual([]);
      expect(summaryLinks()).toEqual(['Name is required.']);
    });

    it('rejects an invalid email address', async () => {
      await type(nameInput(), 'Ann');
      await type(emailInput(), 'ann.example.com');
      await submit();
      expect(requests).toEqual([]);
      expect(summaryLinks()).toEqual([MESSAGES.form.email]);
      expect(emailInput().getAttribute('aria-invalid')).toBe('true');
    });

    it('rejects values above the maximum lengths', async () => {
      await type(nameInput(), 'x'.repeat(101));
      await type(emailInput(), `${'a'.repeat(245)}@example.com`); // 257 characters
      await submit();
      expect(requests).toEqual([]);
      // Angular's email validator already limits an address to 254 characters, so a longer one
      // is reported as not valid rather than as too long.
      expect(summaryLinks()).toEqual([
        'Name must be 100 characters or fewer.',
        MESSAGES.form.email,
      ]);
    });

    it('accepts values at the maximum lengths', async () => {
      await type(nameInput(), 'x'.repeat(100));
      await type(emailInput(), `${'a'.repeat(64)}@${'b'.repeat(63)}.${'c'.repeat(63)}.example.com`);
      expect(emailInput().value.length).toBeLessThanOrEqual(254);
      await submit();
      expect(requests.length).toBe(1);
    });
  });

  describe('successful submit', () => {
    it('sends the trimmed name and the email address', async () => {
      await type(nameInput(), '  Ann Example ');
      await type(emailInput(), 'ann@example.com');
      await submit();
      expect(requests).toEqual([{ name: 'Ann Example', email: 'ann@example.com' }]);
    });

    it('shows a busy state and blocks a second submit while waiting', async () => {
      await fillValid();
      await submit();
      expect(button().getAttribute('aria-disabled')).toBe('true');
      await submit();
      expect(requests.length).toBe(1);
    });

    it('announces the generic confirmation and resets the form (FR-05)', async () => {
      await fillValid();
      await submit();
      response.next();
      response.complete();
      await fixture.whenStable();

      expect(politeText()).toBe(MESSAGES.signup.emailConfirmation);
      expect(alertText()).toBe('');
      expect(nameInput().value).toBe('');
      expect(emailInput().value).toBe('');
      expect(nameInput().getAttribute('aria-invalid')).not.toBe('true');
      expect(el.querySelector('mat-error')).toBeNull();
      expect(el.querySelector('.error-summary')).toBeNull();
      expect(button().hasAttribute('aria-disabled')).toBe(false);
    });

    it('announces the confirmation again after a second sign-up', async () => {
      for (let i = 0; i < 2; i++) {
        await fillValid();
        await submit();
        expect(politeText()).toBe(''); // cleared, so the next message is a change
        response.next();
        response.complete();
        await fixture.whenStable();
        expect(politeText()).toBe(MESSAGES.signup.emailConfirmation);
      }
    });
  });

  describe('failed submit', () => {
    it('shows server field errors on the matching fields and keeps the input', async () => {
      await fillValid();
      await submit();
      response.error(
        new ApiError({
          kind: 'validation',
          status: 400,
          message: MESSAGES.apiError.validation,
          fieldErrors: [{ field: 'email', message: 'The email address is not accepted.' }],
        }),
      );
      await fixture.whenStable();

      expect(errorOf(emailInput())).toBe('The email address is not accepted.');
      expect(summaryLinks()).toEqual(['The email address is not accepted.']);
      expect(document.activeElement).toBe(el.querySelector('.error-summary'));
      expect(alertText()).toBe('');
      expect(nameInput().value).toBe('Ann Example');
      expect(emailInput().value).toBe('ann@example.com');
    });

    it('shows a general message when no server field error matches a field', async () => {
      await fillValid();
      await submit();
      response.error(
        new ApiError({
          kind: 'validation',
          status: 400,
          message: MESSAGES.apiError.validation,
          fieldErrors: [{ field: 'unknown', message: 'x' }],
        }),
      );
      await fixture.whenStable();
      expect(alertText()).toBe(MESSAGES.apiError.validation);
    });

    it('shows a general message for a network error and keeps the input', async () => {
      await fillValid();
      await submit();
      response.error(
        new ApiError({ kind: 'network', status: 0, message: MESSAGES.apiError.network }),
      );
      await fixture.whenStable();

      expect(alertText()).toBe(MESSAGES.apiError.network);
      expect(politeText()).toBe('');
      expect(nameInput().value).toBe('Ann Example');
      expect(emailInput().value).toBe('ann@example.com');
      expect(button().hasAttribute('aria-disabled')).toBe(false);
    });

    it('shows the CSRF message for a failed security check', async () => {
      await fillValid();
      await submit();
      response.error(
        new ApiError({ kind: 'csrf', status: 403, message: MESSAGES.apiError.securityCheckFailed }),
      );
      await fixture.whenStable();
      expect(alertText()).toBe(MESSAGES.apiError.securityCheckFailed);
    });

    it('shows a generic message for an error that is not an ApiError', async () => {
      await fillValid();
      await submit();
      response.error(new Error('boom'));
      await fixture.whenStable();
      expect(alertText()).toBe(MESSAGES.apiError.requestFailed);
    });
  });
});
