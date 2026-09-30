import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type StatusMessageKind = 'success' | 'error';

/**
 * Form outcome message announced to screen readers (FE-10, NFR-13). Success is announced politely
 * (`role="status"`), errors assertively (`role="alert"`). Both live regions are always in the DOM,
 * because screen readers only announce changes inside a region that already exists.
 */
@Component({
  selector: 'app-status-message',
  template: `
    <div role="status" aria-live="polite" aria-atomic="true">
      @if (kind() === 'success' && message()) {
        <p class="status-message status-message--success">{{ message() }}</p>
      }
    </div>
    <div role="alert" aria-live="assertive" aria-atomic="true">
      @if (kind() === 'error' && message()) {
        <p class="status-message status-message--error">{{ message() }}</p>
      }
    </div>
  `,
  styles: `
    .status-message {
      margin: 0 0 1rem;
      padding: 0.75rem 1rem;
      border-radius: 4px;
      border-left: 4px solid;
    }
    .status-message--success {
      background: var(--mat-sys-primary-container);
      color: var(--mat-sys-on-primary-container);
      border-color: var(--mat-sys-primary);
    }
    .status-message--error {
      background: var(--mat-sys-error-container);
      color: var(--mat-sys-on-error-container);
      border-color: var(--mat-sys-error);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusMessage {
  /** Text to show and announce; nothing is shown when empty. */
  readonly message = input<string | null>(null);
  readonly kind = input<StatusMessageKind>('success');
}
