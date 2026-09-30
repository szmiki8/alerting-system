import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatProgressSpinner } from '@angular/material/progress-spinner';
import { MESSAGES } from '../../core/messages';

/**
 * Submit button with a busy state (FE-10, NFR-13). While busy it shows a spinner, stays focusable
 * with `aria-disabled="true"` (a natively disabled button would drop keyboard focus), blocks
 * further submits, including Enter in a text field, and announces the busy text politely.
 * The label is projected: `<app-submit-button [busy]="sending()">Subscribe</app-submit-button>`.
 */
@Component({
  selector: 'app-submit-button',
  imports: [MatButton, MatProgressSpinner],
  template: `
    <button
      mat-flat-button
      type="submit"
      class="submit-button"
      [disabled]="busy()"
      disabledInteractive
      (click)="blockWhileBusy($event)"
    >
      @if (busy()) {
        <mat-progress-spinner
          class="submit-button__spinner"
          mode="indeterminate"
          diameter="18"
          aria-hidden="true"
        />
      }
      <ng-content />
    </button>
    <span class="visually-hidden" role="status">
      @if (busy()) {
        {{ busyText() }}
      }
    </span>
  `,
  styles: `
    .submit-button__spinner {
      display: inline-block;
      margin-right: 0.5rem;
      vertical-align: middle;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SubmitButton {
  readonly busy = input(false);
  /** Announced to screen readers when the button becomes busy. */
  readonly busyText = input<string>(MESSAGES.form.submitting);

  /**
   * Cancels the click while busy. Enter in a text field submits through a synthetic click on the
   * submit button, so cancelling the click blocks every way of submitting the form twice.
   */
  protected blockWhileBusy(event: Event): void {
    if (this.busy()) {
      event.preventDefault();
    }
  }
}
