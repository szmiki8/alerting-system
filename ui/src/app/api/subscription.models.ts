/**
 * Public subscription resources (architecture Section 9.1, FR-01, FR-03).
 * Hand-written until the OpenAPI contract is published (OP-12, FE-27).
 */

/** Body of `POST /api/v1/subscriptions/email`. */
export interface EmailSubscriptionRequest {
  name: string;
  email: string;
}

/** Body of `POST /api/v1/subscriptions/slack`. `label` is left out when empty. */
export interface SlackSubscriptionRequest {
  webhookUrl: string;
  label?: string;
}

/**
 * Body of the HTTP 202 answer to both subscription requests. It is identical for new and already
 * registered addresses (FR-05); the UI shows its own confirmation text.
 */
export interface SubscriptionAcceptedResponse {
  message: string;
}
