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
  /** Messages of the public sign-up pages (FE-11, FE-12). */
  signup: {
    /** Same text for new and already registered addresses (FR-05). */
    emailConfirmation:
      'Thank you. If this address was not subscribed yet, it will now receive an email for each new event.',
    /** Same text for new and already registered webhooks (FR-05). */
    slackConfirmation:
      'Thank you. If this channel was not subscribed yet, it will now receive a Slack message for each new event.',
    webhookUrlFormat:
      'Enter a Slack incoming webhook URL. It starts with https://hooks.slack.com/services/.',
    verifyingWebhook: 'Sending a welcome message to your Slack channel, please wait.',
    /** Slack rejected the welcome message or did not answer (FR-04, HTTP 422). */
    webhookNotVerified:
      'The webhook could not be verified: Slack did not accept our welcome message. Check that you copied the complete webhook URL and that the webhook is still active in Slack, then try again.',
  },
} as const;
