import { MESSAGES } from '../src/app/core/messages';
import { expect, test } from './support/fixtures';
import { ACCEPTED, MockReply, WEBHOOK_NOT_VERIFIED, XSRF_TOKEN } from './support/mock-backend';

/** Slack sign-up against the mocked Core (FE-21: FR-03, FR-04, FR-05). */
const SLACK_PATH = '/api/v1/subscriptions/slack';
const SECRET = 'e2eSecretPart0123456789';
const WEBHOOK_URL = `https://hooks.slack.com/services/T0E2E0001/B0E2E0002/${SECRET}`;

test.describe('Slack sign-up', () => {
  test('subscribes a channel and clears the form', async ({ page, backend }) => {
    // Keep the answer pending until the busy state has been checked.
    let release: () => void = () => undefined;
    const released = new Promise<void>((resolve) => (release = resolve));
    await backend.onPost(SLACK_PATH, async (): Promise<MockReply> => {
      await released;
      return ACCEPTED;
    });
    await backend.open('/slack');

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Get news alerts in Slack');
    await page.getByLabel('Slack webhook URL').fill(WEBHOOK_URL);
    await page.getByLabel('Label').fill('Newsroom');
    await page.getByRole('button', { name: 'Subscribe channel' }).click();

    await expect(
      page.getByRole('status').filter({ hasText: MESSAGES.signup.verifyingWebhook }),
    ).toBeAttached();
    await expect(page.getByRole('button', { name: 'Subscribe channel' })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
    release();

    await expect(
      page.getByRole('status').filter({ hasText: MESSAGES.signup.slackConfirmation }),
    ).toBeVisible();
    await expect(page.getByLabel('Slack webhook URL')).toHaveValue('');
    await expect(page.getByLabel('Label')).toHaveValue('');
    expect(backend.requests).toEqual([
      {
        method: 'POST',
        path: SLACK_PATH,
        xsrfHeader: XSRF_TOKEN,
        body: { webhookUrl: WEBHOOK_URL, label: 'Newsroom' },
      },
    ]);
  });

  test('leaves out an empty label', async ({ page, backend }) => {
    await backend.onPost(SLACK_PATH, ACCEPTED);
    await backend.open('/slack');

    await page.getByLabel('Slack webhook URL').fill(WEBHOOK_URL);
    await page.getByRole('button', { name: 'Subscribe channel' }).click();

    await expect(
      page.getByRole('status').filter({ hasText: MESSAGES.signup.slackConfirmation }),
    ).toBeVisible();
    expect(backend.requests.map((request) => request.body)).toEqual([{ webhookUrl: WEBHOOK_URL }]);
    expect(backend.requests[0]?.xsrfHeader).toBe(XSRF_TOKEN);
  });

  test('rejects a URL that is not a Slack incoming webhook', async ({ page, backend }) => {
    await backend.onPost(SLACK_PATH, ACCEPTED);
    await backend.open('/slack');

    await page.getByLabel('Slack webhook URL').fill('https://example.com/services/T1/B1/abc');
    await page.getByRole('button', { name: 'Subscribe channel' }).click();

    const summary = page.getByRole('alert').filter({ hasText: MESSAGES.form.errorSummaryTitle });
    await expect(summary.getByRole('link')).toHaveText([MESSAGES.signup.webhookUrlFormat]);
    await expect(
      page.getByRole('textbox', { name: 'Slack webhook URL' }),
    ).toHaveAccessibleDescription(new RegExp(escapeRegExp(MESSAGES.signup.webhookUrlFormat)));
    expect(backend.requests).toEqual([]);
  });

  test('explains a failed verification and keeps the URL', async ({ page, backend }) => {
    await backend.onPost(SLACK_PATH, WEBHOOK_NOT_VERIFIED);
    await backend.open('/slack');

    await page.getByLabel('Slack webhook URL').fill(WEBHOOK_URL);
    await page.getByRole('button', { name: 'Subscribe channel' }).click();

    await expect(
      page.getByRole('alert').filter({ hasText: MESSAGES.signup.webhookNotVerified }),
    ).toBeVisible();
    await expect(page.getByLabel('Slack webhook URL')).toHaveValue(WEBHOOK_URL);
    // The secret part of the URL appears nowhere on the page outside the field.
    await expect(page.getByText(SECRET)).toHaveCount(0);
    expect(backend.requests).toHaveLength(1);
  });

  test('shows a network error and keeps the input', async ({ page, backend }) => {
    const consoleTexts: string[] = [];
    page.on('console', (message) => consoleTexts.push(message.text()));
    await backend.onPost(SLACK_PATH, 'network-error');
    await backend.open('/slack');

    await page.getByLabel('Slack webhook URL').fill(WEBHOOK_URL);
    await page.getByRole('button', { name: 'Subscribe channel' }).click();

    await expect(
      page.getByRole('alert').filter({ hasText: MESSAGES.apiError.network }),
    ).toBeVisible();
    await expect(page.getByLabel('Slack webhook URL')).toHaveValue(WEBHOOK_URL);
    // The browser itself logs the failed request with its URL, but not the body.
    expect(consoleTexts.join('\n')).not.toContain(SECRET);
  });
});

function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
