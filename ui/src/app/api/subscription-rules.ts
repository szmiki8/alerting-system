/**
 * Input rules of the public subscription resources, mirrored by the sign-up forms (FE-11, FE-12)
 * so that invalid input is rejected before a request is sent. The Core stays the authority: it
 * validates again and its field errors are shown on the fields.
 *
 * The values mirror the Core's subscriber types: `EmailSubscriberType` (BE-15, module
 * `channel-email`) and `SlackWebhookUrl` / `SlackSubscriberType` (BE-18, module `channel-slack`).
 * Change them together with the Core. This file is the only place in the UI that holds them.
 */
export const SUBSCRIPTION_RULES = {
  email: {
    /** `name`: required. */
    nameMaxLength: 100,
    /** `email`: required, email format; 254 is the practical maximum of an address (RFC 5321). */
    emailMaxLength: 254,
  },
  slack: {
    /**
     * `webhookUrl`: required and must be a Slack incoming webhook URL. The strict pattern also
     * keeps the Core from calling arbitrary hosts (server-side request forgery, architecture
     * Section 9.1). Anchored, so `Validators.pattern` matches the whole value.
     */
    webhookUrlPattern:
      /^https:\/\/hooks\.slack\.com(?::443)?\/services\/T[A-Z0-9]{8,20}\/B[A-Z0-9]{8,20}\/[A-Za-z0-9]{20,64}\/?$/,
    /** `label`: optional, at most 100 characters after trimming. */
    labelMaxLength: 100,
  },
} as const;
