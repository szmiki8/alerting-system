import { AbstractControl, FormGroup } from '@angular/forms';
import { FieldError } from '../../api/problem-details';
import { MESSAGES } from '../../core/messages';
import { ErrorSummaryEntry } from './error-summary';

/** Error key under which a server-side field error is stored on a form control. */
export const SERVER_ERROR_KEY = 'server';

/** A form field as the error summary sees it. */
export interface FormFieldInfo {
  /** Control path in the form group, for example `email`. */
  path: string;
  /** `id` of the input element the summary entry links to. */
  id: string;
  /** Visible field label, used in messages. */
  label: string;
  /** Field-specific texts by validator error key, for example a format hint for `pattern`. */
  messages?: FieldMessages;
}

/** Field-specific error texts by validator error key (`required`, `pattern`, ...). */
export type FieldMessages = Readonly<Partial<Record<string, string>>>;

/**
 * Copies the server's field errors (from an `ApiError` of kind `validation`) onto the matching
 * controls with `setErrors`, and marks them touched so Material shows them in the field's
 * `mat-error` slot. The server error disappears on the next edit, when the control's own
 * validators run again. By default a server field name is the control path; `fieldToPath` maps
 * names that differ. Returns the errors that match no control, so the form can show them as a
 * general message.
 */
export function applyServerFieldErrors(
  form: FormGroup,
  fieldErrors: readonly FieldError[],
  fieldToPath: Readonly<Record<string, string>> = {},
): FieldError[] {
  const unmatched: FieldError[] = [];
  for (const error of fieldErrors) {
    const control = form.get(fieldToPath[error.field] ?? error.field);
    if (control) {
      control.setErrors({ ...control.errors, [SERVER_ERROR_KEY]: error.message });
      control.markAsTouched();
    } else {
      unmatched.push(error);
    }
  }
  return unmatched;
}

/**
 * Message for the first error of a control, from the text catalogue; server errors carry their
 * own message. `messages` replaces the catalogue text for single validators of this field.
 * Returns `null` when the control is valid.
 */
export function fieldErrorMessage(
  control: AbstractControl,
  label: string,
  messages: FieldMessages = {},
): string | null {
  const errors = control.errors;
  if (!errors) {
    return null;
  }
  const custom = ['required', 'email', 'maxlength', 'pattern'].find((key) => errors[key]);
  const customMessage = custom ? messages[custom] : undefined;
  if (customMessage) {
    return customMessage;
  }
  if (errors['required']) {
    return MESSAGES.form.required(label);
  }
  if (errors['email']) {
    return MESSAGES.form.email;
  }
  const maxLength = errors['maxlength'] as { requiredLength: number } | undefined;
  if (maxLength) {
    return MESSAGES.form.maxLength(label, maxLength.requiredLength);
  }
  if (errors['pattern']) {
    return MESSAGES.form.pattern(label);
  }
  const serverMessage: unknown = errors[SERVER_ERROR_KEY];
  if (typeof serverMessage === 'string') {
    return serverMessage;
  }
  return MESSAGES.form.invalid(label);
}

/** Error summary entries for all invalid fields, in the order of `fields`. */
export function errorSummaryEntries(
  form: FormGroup,
  fields: readonly FormFieldInfo[],
): ErrorSummaryEntry[] {
  return fields.flatMap((field) => {
    const control = form.get(field.path);
    const message = control ? fieldErrorMessage(control, field.label, field.messages) : null;
    return message ? [{ fieldId: field.id, message }] : [];
  });
}
