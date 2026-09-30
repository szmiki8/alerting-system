import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  FormGroupDirective,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { finalize } from 'rxjs';
import { ApiError } from '../../api/api-error';
import { SubscriptionApiService } from '../../api/subscription-api.service';
import { SUBSCRIPTION_RULES } from '../../api/subscription-rules';
import { MESSAGES } from '../../core/messages';
import { ErrorSummary, ErrorSummaryEntry } from '../../shared/forms/error-summary';
import {
  FormFieldInfo,
  applyServerFieldErrors,
  errorSummaryEntries,
  fieldErrorMessage,
} from '../../shared/forms/form-errors';
import { StatusMessage, StatusMessageKind } from '../../shared/forms/status-message';
import { SubmitButton } from '../../shared/forms/submit-button';

/** Fields in page order; the server's field names (`name`, `email`) are the control paths. */
const FIELDS = {
  name: { path: 'name', id: 'email-signup-name', label: 'Name' },
  email: { path: 'email', id: 'email-signup-email', label: 'Email' },
} as const satisfies Record<string, FormFieldInfo>;

/** Treats a value of only spaces like an empty one, as the Core does for a required text. */
function requiredText(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() === '' ? { required: true } : null;
}

/**
 * Email sign-up, the default public page (FE-11, FR-01, FR-02, FR-05, NFR-13).
 *
 * Client validation mirrors the Core's rules ({@link SUBSCRIPTION_RULES}), so invalid input never
 * leaves the browser. The Core validates again; its field errors are shown on the fields, other
 * failures as one general message that keeps the input.
 */
@Component({
  selector: 'app-email-signup-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    ErrorSummary,
    StatusMessage,
    SubmitButton,
  ],
  templateUrl: './email-signup-page.html',
  styleUrl: '../signup-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmailSignupPage {
  protected readonly fields = FIELDS;

  protected readonly form = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      // Validators.required also makes Material mark the field as required (aria-required).
      validators: [
        Validators.required,
        requiredText,
        Validators.maxLength(SUBSCRIPTION_RULES.email.nameMaxLength),
      ],
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.email,
        Validators.maxLength(SUBSCRIPTION_RULES.email.emailMaxLength),
      ],
    }),
  });

  protected readonly busy = signal(false);
  protected readonly status = signal<string | null>(null);
  protected readonly statusKind = signal<StatusMessageKind>('success');
  protected readonly summary = signal<readonly ErrorSummaryEntry[]>([]);
  protected readonly message = fieldErrorMessage;

  private readonly api = inject(SubscriptionApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly errorSummary = viewChild.required(ErrorSummary);
  private readonly formDirective = viewChild.required(FormGroupDirective);

  protected submit(): void {
    if (this.busy()) {
      return;
    }
    // Cleared first, so that a repeated identical message is announced again.
    this.status.set(null);
    if (this.form.invalid) {
      this.showFieldErrors();
      return;
    }
    this.summary.set([]);
    this.busy.set(true);
    const { name, email } = this.form.getRawValue();
    this.api
      .subscribeEmail({ name: name.trim(), email })
      .pipe(
        finalize(() => this.busy.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => this.succeed(),
        error: (error: unknown) => this.fail(error),
      });
  }

  private succeed(): void {
    // resetForm() also clears the "submitted" state, so the empty fields show no errors.
    this.formDirective().resetForm();
    this.showStatus('success', MESSAGES.signup.emailConfirmation);
  }

  private fail(error: unknown): void {
    if (error instanceof ApiError && error.kind === 'validation') {
      const unmatched = applyServerFieldErrors(this.form, error.fieldErrors);
      this.showFieldErrors();
      if (unmatched.length === error.fieldErrors.length) {
        // No field could be marked, so say at least that the input was rejected.
        this.showStatus('error', error.message);
      }
      return;
    }
    this.showStatus(
      'error',
      error instanceof ApiError ? error.message : MESSAGES.apiError.requestFailed,
    );
  }

  /** Marks all fields, lists the invalid ones and moves focus to the list (NFR-13). */
  private showFieldErrors(): void {
    this.form.markAllAsTouched();
    this.summary.set(errorSummaryEntries(this.form, Object.values(FIELDS)));
    this.errorSummary().focus();
  }

  private showStatus(kind: StatusMessageKind, text: string): void {
    this.statusKind.set(kind);
    this.status.set(text);
  }
}
