import { Page, Request, Route } from '@playwright/test';

/**
 * A mocked Core for the end-to-end tests (FE-21). It answers the Core's API through Playwright
 * request interception, following the API contract (architecture Section 9, BE-12, BE-13):
 *
 * - `GET /api/v1/csrf` answers 204 and sets the `XSRF-TOKEN` cookie, as the Core does (OP-01).
 * - A POST without the matching `X-XSRF-TOKEN` header gets the Core's 403 CSRF problem, so a
 *   missing header makes the test fail; tests also check the recorded header explicitly.
 * - Any other `/api` call that a test did not mock gets a 501 problem and is recorded in
 *   {@link unexpected}, so a forgotten mock cannot pass silently.
 */

export const XSRF_TOKEN = 'e2e-xsrf-token-5f1c';

const PROBLEM_JSON = 'application/problem+json';

/** A reply of the mocked Core: an HTTP answer, or a connection failure. */
export type MockReply = { status: number; body?: object; contentType?: string } | 'network-error';

/** One request as the mocked Core received it. */
export interface RecordedRequest {
  method: string;
  path: string;
  xsrfHeader: string | undefined;
  body: unknown;
}

export class MockBackend {
  /** Requests to mocked resources, in arrival order. */
  readonly requests: RecordedRequest[] = [];
  /** Requests to `/api` paths that no test mocked. */
  readonly unexpected: string[] = [];

  constructor(private readonly page: Page) {}

  /** Installs the catch-all and the CSRF endpoint. Call before the first navigation. */
  async install(): Promise<void> {
    // Playwright tries the most recently registered route first, so the catch-all comes first.
    await this.page.route(
      (url) => url.pathname.startsWith('/api/'),
      (route) => {
        this.unexpected.push(
          `${route.request().method()} ${new URL(route.request().url()).pathname}`,
        );
        return route.fulfill(problem(501, 'urn:alerting:problem:not-mocked', 'Not mocked'));
      },
    );
    await this.page.route(
      (url) => url.pathname === '/api/v1/csrf',
      (route) =>
        route.fulfill({
          status: 204,
          headers: { 'Set-Cookie': `XSRF-TOKEN=${XSRF_TOKEN}; Path=/; SameSite=Lax` },
        }),
    );
  }

  /**
   * Answers POSTs to `path` with `reply`. A function reply may wait, for example to keep the
   * request pending while a test checks the busy state.
   */
  async onPost(
    path: string,
    reply: MockReply | ((request: RecordedRequest) => MockReply | Promise<MockReply>),
  ): Promise<void> {
    await this.page.route(
      (url) => url.pathname === path,
      async (route) => {
        const request = route.request();
        if (request.method() !== 'POST') {
          return route.fallback();
        }
        const recorded = record(request);
        this.requests.push(recorded);
        if (recorded.xsrfHeader !== XSRF_TOKEN) {
          return route.fulfill(
            problem(403, 'urn:alerting:problem:csrf-token-invalid', 'Forbidden'),
          );
        }
        const answer = typeof reply === 'function' ? await reply(recorded) : reply;
        return fulfill(route, answer);
      },
    );
  }

  /** Opens a page of the UI and waits until the CSRF cookie has been set. */
  async open(path: string): Promise<void> {
    const csrf = this.page.waitForResponse(
      (response) => new URL(response.url()).pathname === '/api/v1/csrf',
    );
    await this.page.goto(path);
    await csrf;
  }
}

/** A 202 answer as the Core sends it for new and existing addresses alike (FR-05). */
export const ACCEPTED: MockReply = {
  status: 202,
  body: { message: 'Thank you. If the address is new, it has been subscribed.' },
  contentType: 'application/json',
};

/** A 400 validation problem with field errors (BE-12). */
export function validationProblem(errors: { field: string; message: string }[]): MockReply {
  return {
    status: 400,
    contentType: PROBLEM_JSON,
    body: {
      type: 'urn:alerting:problem:validation',
      title: 'Invalid request',
      status: 400,
      detail: 'The request has invalid fields.',
      errors,
    },
  };
}

/** The 422 answer when Slack did not accept the welcome message (FR-04, BE-19). */
export const WEBHOOK_NOT_VERIFIED: MockReply = {
  status: 422,
  contentType: PROBLEM_JSON,
  body: {
    type: 'urn:alerting:problem:webhook-not-verified',
    title: 'Webhook not verified',
    status: 422,
    detail: 'The welcome message could not be delivered.',
  },
};

function record(request: Request): RecordedRequest {
  return {
    method: request.method(),
    path: new URL(request.url()).pathname,
    xsrfHeader: request.headers()['x-xsrf-token'],
    body: request.postDataJSON() as unknown,
  };
}

function fulfill(route: Route, reply: MockReply): Promise<void> {
  if (reply === 'network-error') {
    return route.abort('connectionrefused');
  }
  return route.fulfill({
    status: reply.status,
    contentType: reply.contentType ?? PROBLEM_JSON,
    body: reply.body === undefined ? '' : JSON.stringify(reply.body),
  });
}

function problem(status: number, type: string, title: string) {
  return {
    status,
    contentType: PROBLEM_JSON,
    body: JSON.stringify({ type, title, status }),
  };
}

/** Text of the element that has keyboard focus, to check where focus went after an action. */
export function focusedText(page: Page): Promise<string> {
  return page.evaluate(() => document.activeElement?.textContent?.trim() ?? '');
}
