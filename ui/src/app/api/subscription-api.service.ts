import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_PATHS } from './api-paths';
import {
  EmailSubscriptionRequest,
  SlackSubscriptionRequest,
  SubscriptionAcceptedResponse,
} from './subscription.models';

/**
 * Public subscription resources (FE-09, architecture Sections 9.1, 10.1 and 10.2).
 *
 * Both methods complete with `void` on HTTP 202. The answer is the same for new and already
 * registered addresses (FR-05), so nothing from it is passed on; the pages show their own text.
 * Failures arrive as `ApiError` from the error interceptor (FE-08): kind `validation` with field
 * errors for 400, kind `problem` with type `webhook-not-verified` for the Slack 422 (FR-04).
 */
@Injectable({ providedIn: 'root' })
export class SubscriptionApiService {
  private readonly http = inject(HttpClient);

  /** `POST /api/v1/subscriptions/email`. */
  subscribeEmail(request: EmailSubscriptionRequest): Observable<void> {
    const body: EmailSubscriptionRequest = { name: request.name, email: request.email };
    return this.post(API_PATHS.emailSubscriptions, body);
  }

  /** `POST /api/v1/subscriptions/slack`. An empty or blank label is left out of the body. */
  subscribeSlack(request: SlackSubscriptionRequest): Observable<void> {
    const label = request.label?.trim();
    const body: SlackSubscriptionRequest = label
      ? { webhookUrl: request.webhookUrl, label }
      : { webhookUrl: request.webhookUrl };
    return this.post(API_PATHS.slackSubscriptions, body);
  }

  private post(url: string, body: object): Observable<void> {
    return this.http.post<SubscriptionAcceptedResponse>(url, body).pipe(map(() => undefined));
  }
}
