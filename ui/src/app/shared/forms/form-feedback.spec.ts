import { ChangeDetectionStrategy, Component, signal, viewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MESSAGES } from '../../core/messages';
import { ErrorSummary, ErrorSummaryEntry } from './error-summary';
import {
  FormFieldInfo,
  applyServerFieldErrors,
  errorSummaryEntries,
  fieldErrorMessage,
} from './form-errors';
import { StatusMessage, StatusMessageKind } from './status-message';
import { SubmitButton } from './submit-button';

const FIELDS: readonly FormFieldInfo[] = [
  { path: 'name', id: 'host-name', label: 'Name' },
  { path: 'email', id: 'host-email', label: 'Email' },
];

/** A sign-up-like form that uses all FE-10 pieces the way FE-11 and FE-12 will. */
@Component({
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    ErrorSummary,
    StatusMessage,
    SubmitButton,
  ],
  template: `
    <app-status-message [message]="status()" [kind]="statusKind()" />
    <app-error-summary [entries]="summary()" />
    <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
      <mat-form-field>
        <mat-label>Name</mat-label>
        <input matInput id="host-name" formControlName="name" />
        <mat-error>{{ message(form.controls.name, 'Name') }}</mat-error>
      </mat-form-field>
      <mat-form-field>
        <mat-label>Email</mat-label>
        <input matInput id="host-email" type="email" formControlName="email" />
        <mat-error>{{ message(form.controls.email, 'Email') }}</mat-error>
      </mat-form-field>
      <app-submit-button [busy]="busy()">Subscribe</app-submit-button>
    </form>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
class HostForm {
  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(20)],
    }),
  });
  readonly status = signal<string | null>(null);
  readonly statusKind = signal<StatusMessageKind>('success');
  readonly summary = signal<ErrorSummaryEntry[]>([]);
  readonly busy = signal(false);
  submits = 0;

  private readonly errorSummary = viewChild.required(ErrorSummary);

  protected readonly message = fieldErrorMessage;

  submit(): void {
    this.submits++;
    this.showErrors();
  }

  showErrors(): void {
    this.summary.set(errorSummaryEntries(this.form, FIELDS));
    if (this.form.invalid) {
      this.errorSummary().focus();
    }
  }
}

describe('Form feedback components', () => {
  let fixture: ComponentFixture<HostForm>;
  let host: HostForm;
  let el: HTMLElement;

  beforeEach(async () => {
    fixture = TestBed.createComponent(HostForm);
    host = fixture.componentInstance;
    el = fixture.nativeElement;
    document.body.appendChild(el); // focus needs attached elements
    await fixture.whenStable();
  });

  afterEach(() => document.body.replaceChildren());

  const button = () => el.querySelector<HTMLButtonElement>('app-submit-button button')!;
  const input = (id: string) => el.querySelector<HTMLInputElement>(`#${id}`)!;

  async function type(id: string, value: string): Promise<void> {
    const field = input(id);
    field.value = value;
    field.dispatchEvent(new Event('input'));
    field.dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  }

  describe('StatusMessage', () => {
    const politeRegion = () => el.querySelector('app-status-message [role="status"]')!;
    const alertRegion = () => el.querySelector('app-status-message [role="alert"]')!;

    it('keeps both live regions in the DOM before any message is shown', () => {
      expect(politeRegion().getAttribute('aria-live')).toBe('polite');
      expect(alertRegion().getAttribute('aria-live')).toBe('assertive');
      expect(politeRegion().textContent?.trim()).toBe('');
      expect(alertRegion().textContent?.trim()).toBe('');
    });

    it('announces success politely', async () => {
      host.status.set('Thank you. Check your inbox.');
      await fixture.whenStable();
      expect(politeRegion().textContent?.trim()).toBe('Thank you. Check your inbox.');
      expect(alertRegion().textContent?.trim()).toBe('');
    });

    it('announces errors assertively', async () => {
      host.statusKind.set('error');
      host.status.set(MESSAGES.apiError.network);
      await fixture.whenStable();
      expect(alertRegion().textContent?.trim()).toBe(MESSAGES.apiError.network);
      expect(politeRegion().textContent?.trim()).toBe('');
    });
  });

  describe('ErrorSummary', () => {
    it('lists every invalid field after a failed submit and moves focus to the summary', async () => {
      button().click();
      await fixture.whenStable();

      const summary = el.querySelector<HTMLElement>('.error-summary')!;
      expect(summary.querySelector('h2')?.textContent).toBe(MESSAGES.form.errorSummaryTitle);
      expect(summary.getAttribute('aria-labelledby')).toBe(summary.querySelector('h2')?.id);
      expect(summary.querySelector('[role="alert"]')).not.toBeNull();
      const links = Array.from(summary.querySelectorAll('a'));
      expect(links.map((a) => [a.getAttribute('href'), a.textContent?.trim()])).toEqual([
        ['#host-name', 'Name is required.'],
        ['#host-email', 'Email is required.'],
      ]);
      expect(document.activeElement).toBe(summary);
    });

    it('moves focus to the field when an entry is activated', async () => {
      button().click();
      await fixture.whenStable();
      el.querySelector<HTMLAnchorElement>('.error-summary a[href="#host-email"]')!.click();
      expect(document.activeElement).toBe(input('host-email'));
    });

    it('is not shown when there are no errors', async () => {
      await type('host-name', 'Ann');
      await type('host-email', 'ann@example.com');
      button().click();
      await fixture.whenStable();
      expect(el.querySelector('.error-summary')).toBeNull();
      expect(host.submits).toBe(1);
    });
  });

  describe('SubmitButton', () => {
    it('is a normal submit button when idle', () => {
      expect(button().type).toBe('submit');
      expect(button().textContent?.trim()).toBe('Subscribe');
      expect(button().hasAttribute('aria-disabled')).toBe(false);
      expect(el.querySelector('mat-progress-spinner')).toBeNull();
    });

    it('shows a busy state that assistive technology can perceive', async () => {
      host.busy.set(true);
      await fixture.whenStable();
      expect(button().getAttribute('aria-disabled')).toBe('true');
      expect(button().disabled).toBe(false); // stays focusable
      expect(el.querySelector('mat-progress-spinner')?.getAttribute('aria-hidden')).toBe('true');
      expect(el.querySelector('app-submit-button [role="status"]')?.textContent?.trim()).toBe(
        MESSAGES.form.submitting,
      );
    });

    it('blocks further submits while busy', async () => {
      button().click();
      await fixture.whenStable();
      expect(host.submits).toBe(1);

      host.busy.set(true);
      await fixture.whenStable();
      button().click();
      button().click();
      expect(host.submits).toBe(1);

      host.busy.set(false);
      await fixture.whenStable();
      button().click();
      expect(host.submits).toBe(2);
    });
  });

  describe('server field errors', () => {
    it('sets server errors on the matching controls and returns the unmatched ones', async () => {
      await type('host-name', 'Ann');
      await type('host-email', 'ann@example.com');

      const unmatched = applyServerFieldErrors(host.form, [
        { field: 'email', message: 'must be a well-formed email address' },
        { field: 'unknownField', message: 'is not allowed' },
      ]);
      host.showErrors();
      await fixture.whenStable();

      expect(unmatched).toEqual([{ field: 'unknownField', message: 'is not allowed' }]);
      expect(host.form.controls.email.errors).toEqual({
        server: 'must be a well-formed email address',
      });
      expect(host.form.controls.name.valid).toBe(true);

      // Shown in the Material error slot, which the input references with aria-describedby.
      const error = el.querySelector<HTMLElement>('mat-error')!;
      expect(error.textContent?.trim()).toBe('must be a well-formed email address');
      expect(input('host-email').getAttribute('aria-describedby')).toContain(error.id);
      expect(input('host-email').getAttribute('aria-invalid')).toBe('true');
      // And listed in the error summary.
      expect(el.querySelector('.error-summary a')?.textContent?.trim()).toBe(
        'must be a well-formed email address',
      );
    });

    it('maps differing server field names through the field map', () => {
      applyServerFieldErrors(host.form, [{ field: 'emailAddress', message: 'taken' }], {
        emailAddress: 'email',
      });
      expect(host.form.controls.email.hasError('server')).toBe(true);
      expect(host.form.controls.email.touched).toBe(true);
    });

    it('clears the server error when the user edits the field', async () => {
      applyServerFieldErrors(host.form, [{ field: 'name', message: 'not accepted' }]);
      expect(host.form.controls.name.hasError('server')).toBe(true);
      await type('host-name', 'Ann');
      expect(host.form.controls.name.errors).toBeNull();
    });
  });

  describe('fieldErrorMessage', () => {
    it('turns validator errors into catalogue messages', () => {
      const control = new FormControl('');
      expect(fieldErrorMessage(control, 'Name')).toBeNull();
      control.setErrors({ required: true });
      expect(fieldErrorMessage(control, 'Name')).toBe('Name is required.');
      control.setErrors({ email: true });
      expect(fieldErrorMessage(control, 'Email')).toBe(MESSAGES.form.email);
      control.setErrors({ maxlength: { requiredLength: 80, actualLength: 81 } });
      expect(fieldErrorMessage(control, 'Label')).toBe('Label must be 80 characters or fewer.');
      control.setErrors({ pattern: { requiredPattern: 'x' } });
      expect(fieldErrorMessage(control, 'Slack webhook URL')).toBe(
        'Slack webhook URL is not in the correct format.',
      );
      control.setErrors({ server: 'from the server' });
      expect(fieldErrorMessage(control, 'Name')).toBe('from the server');
      control.setErrors({ somethingElse: true });
      expect(fieldErrorMessage(control, 'Name')).toBe('Name is not valid.');
    });

    it('uses field-specific texts for single validators', () => {
      const control = new FormControl('');
      const messages = { pattern: 'Enter a Slack webhook URL.' };
      control.setErrors({ pattern: { requiredPattern: 'x' } });
      expect(fieldErrorMessage(control, 'URL', messages)).toBe('Enter a Slack webhook URL.');
      control.setErrors({ required: true, pattern: true });
      expect(fieldErrorMessage(control, 'URL', messages)).toBe('URL is required.');
      control.setErrors({ server: 'from the server' });
      expect(fieldErrorMessage(control, 'URL', { server: 'ignored' })).toBe('from the server');
    });

    it('uses field-specific texts in the error summary', () => {
      const form = new FormGroup({ url: new FormControl('x', Validators.pattern(/^y$/)) });
      expect(
        errorSummaryEntries(form, [
          { path: 'url', id: 'url', label: 'URL', messages: { pattern: 'Custom.' } },
        ]),
      ).toEqual([{ fieldId: 'url', message: 'Custom.' }]);
    });
  });
});
