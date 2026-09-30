import {
  ChangeDetectionStrategy,
  Component,
  DOCUMENT,
  ElementRef,
  Injector,
  afterNextRender,
  inject,
  input,
  viewChild,
} from '@angular/core';
import { MESSAGES } from '../../core/messages';

/** One invalid field in the error summary. `fieldId` is the `id` of the field's input element. */
export interface ErrorSummaryEntry {
  fieldId: string;
  message: string;
}

let nextId = 0;

/**
 * List of all invalid fields after a failed submit (FE-10, NFR-13), following the GOV.UK error
 * summary pattern: the form calls {@link focus} after a failed submit, so screen readers read the
 * summary, and each entry moves focus to its field.
 */
@Component({
  selector: 'app-error-summary',
  template: `
    @if (entries().length > 0) {
      <div #summary class="error-summary" tabindex="-1" [attr.aria-labelledby]="titleId">
        <div role="alert">
          <h2 class="error-summary__title" [id]="titleId">{{ title() }}</h2>
          <ul class="error-summary__list">
            @for (entry of entries(); track entry.fieldId) {
              <li>
                <a [href]="'#' + entry.fieldId" (click)="focusField($event, entry.fieldId)">{{
                  entry.message
                }}</a>
              </li>
            }
          </ul>
        </div>
      </div>
    }
  `,
  styles: `
    .error-summary {
      margin: 0 0 1.5rem;
      padding: 1rem;
      border: 3px solid var(--mat-sys-error);
      border-radius: 4px;
    }
    .error-summary__title {
      margin: 0 0 0.5rem;
      font: var(--mat-sys-title-medium);
    }
    .error-summary__list {
      margin: 0;
      padding-left: 1.25rem;
    }
    .error-summary__list a {
      color: var(--mat-sys-error);
      font-weight: 500;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ErrorSummary {
  readonly entries = input<readonly ErrorSummaryEntry[]>([]);
  readonly title = input<string>(MESSAGES.form.errorSummaryTitle);

  protected readonly titleId = `app-error-summary-title-${nextId++}`;
  private readonly summary = viewChild<ElementRef<HTMLElement>>('summary');
  private readonly document = inject(DOCUMENT);
  private readonly injector = inject(Injector);

  /** Moves focus to the summary once the current entries are rendered. */
  focus(): void {
    afterNextRender(() => this.summary()?.nativeElement.focus(), { injector: this.injector });
  }

  /** Focuses the field in place; following the `#id` link would change the URL (base href). */
  protected focusField(event: Event, fieldId: string): void {
    event.preventDefault();
    this.document.getElementById(fieldId)?.focus();
  }
}
