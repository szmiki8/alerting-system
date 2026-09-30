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
  FormControl,
  FormGroup,
  FormGroupDirective,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { finalize } from 'rxjs';
import { ApiError } from '../../api/api-error';
import { PROBLEM_TYPES } from '../../api/problem-details';
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

/** Fields in page order; the server's field names (`webhookUrl`, `label`) are the control paths. */
const FIELDS = {
  webhookUrl: {
    path: 'webhookUrl',
    id: 'slack-signup-webhook-url',
    label: 'Slack webhook URL',
    messages: { pattern: MESSAGES.signup.webhookUrlFormat },
  },
  label: { path: 'label', id: 'slack-signup-label', label: 'Label' },
} as const satisfies Record<string, FormFieldInfo>;

/** Official Slack documentation on creating an incoming webhook (checked 2026-09-30). */
const SLACK_WEBHOOK_DOCS_URL =
  'https://docs.slack.dev/messaging/sending-messages-using-incoming-webhooks/';

/**
 * Slack sign-up (FE-12, FR-03, FR-04, FR-05, NFR-04, NFR-13).
 *
 * The client only checks the webhook URL format ({@link SUBSCRIPTION_RULES}); the Core verifies
 * the webhook by sending a welcome message before it stores anything. The webhook URL is a
 * secret: it is never logged or shown in a message, and the form is cleared after success.
 */
@Component({
  selector: 'app-slack-signup-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    ErrorSummary,
    StatusMessage,
    SubmitButton,
  ],
  templateUrl: './slack-signup-page.html',
  styleUrl: '../signup-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SlackSignupPage {
  protected readonly fields = FIELDS;
  protected readonly docsUrl = SLACK_WEBHOOK_DOCS_URL;
  protected readonly verifyingText = MESSAGES.signup.verifyingWebhook;

  protected readonly form = new FormGroup({
    webhookUrl: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.pattern(SUBSCRIPTION_RULES.slack.webhookUrlPattern),
      ],
    }),
    label: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(SUBSCRIPTION_RULES.slack.labelMaxLength)],
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
    const { webhookUrl, label } = this.form.getRawValue();
    this.api
      .subscribeSlack({ webhookUrl, label })
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
    // Clears the webhook URL, so the secret does not stay on the screen (NFR-04), and the
    // "submitted" state, so the empty fields show no errors.
    this.formDirective().resetForm();
    this.showStatus('success', MESSAGES.signup.slackConfirmation);
  }

  /** On every failure the input stays, so the visitor can correct the URL. */
  private fail(error: unknown): void {
    if (!(error instanceof ApiError)) {
      this.showStatus('error', MESSAGES.apiError.requestFailed);
      return;
    }
    if (error.kind === 'validation') {
      const unmatched = applyServerFieldErrors(this.form, error.fieldErrors);
      this.showFieldErrors();
      if (unmatched.length === error.fieldErrors.length) {
        // No field could be marked, so say at least that the input was rejected.
        this.showStatus('error', error.message);
      }
      return;
    }
    // Slack did not accept the welcome message; nothing was stored (FR-04). The text gives
    // guidance and says nothing about the URL itself.
    const notVerified = error.status === 422 || error.type === PROBLEM_TYPES.webhookNotVerified;
    this.showStatus('error', notVerified ? MESSAGES.signup.webhookNotVerified : error.message);
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
