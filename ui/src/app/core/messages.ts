/**
 * UI text catalogue (ASM-09, OP-14).
 *
 * Every user-facing message that is produced in TypeScript lives here: validation messages, API
 * error messages and notifications. Static page text stays in the templates (architecture
 * Section 11). Keeping these texts in one file lets a translation mechanism be added later.
 */
export const MESSAGES = {
  /** Messages for failed API calls (FE-08). */
  apiError: {
    network: 'We could not reach the server. Check your connection and try again.',
    server: 'Something went wrong on our side. Please try again later.',
    validation: 'Some of the entered data is not valid. Please correct the marked fields.',
    sessionExpired: 'Your session has expired. Please sign in again.',
    securityCheckFailed: 'Your session has expired. Please reload the page and try again.',
    requestFailed: 'The request could not be completed. Please try again.',
  },
  /** Messages for form fields and form feedback (FE-10). */
  form: {
    required: (label: string) => `${label} is required.`,
    email: 'Enter a valid email address, like name@example.com.',
    maxLength: (label: string, max: number) => `${label} must be ${max} characters or fewer.`,
    pattern: (label: string) => `${label} is not in the correct format.`,
    invalid: (label: string) => `${label} is not valid.`,
    errorSummaryTitle: 'There is a problem',
    submitting: 'Sending, please wait.',
  },
} as const;
