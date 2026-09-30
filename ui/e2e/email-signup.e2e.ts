import { MESSAGES } from '../src/app/core/messages';
import { expect, test } from './support/fixtures';
import { ACCEPTED, XSRF_TOKEN, focusedText, validationProblem } from './support/mock-backend';

/** Email sign-up against the mocked Core (FE-21: FR-01, FR-02, FR-05). */
const EMAIL_PATH = '/api/v1/subscriptions/email';

test.describe('Email sign-up', () => {
  test('subscribes and shows the generic confirmation', async ({ page, backend }) => {
    await backend.onPost(EMAIL_PATH, ACCEPTED);
    await backend.open('/');

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Get news alerts by email');
    await page.getByLabel('Name').fill('Ann Example');
    await page.getByLabel('Email').fill('ann@example.com');
    await page.getByRole('button', { name: 'Subscribe' }).click();

    await expect(
      page.getByRole('status').filter({ hasText: MESSAGES.signup.emailConfirmation }),
    ).toBeVisible();
    await expect(page.getByLabel('Name')).toHaveValue('');
    await expect(page.getByLabel('Email')).toHaveValue('');
    expect(backend.requests).toEqual([
      {
        method: 'POST',
        path: EMAIL_PATH,
        xsrfHeader: XSRF_TOKEN,
        body: { name: 'Ann Example', email: 'ann@example.com' },
      },
    ]);
  });

  test('rejects invalid input on the client without a request', async ({ page, backend }) => {
    await backend.onPost(EMAIL_PATH, ACCEPTED);
    await backend.open('/');
    const summary = page.getByRole('alert').filter({ hasText: MESSAGES.form.errorSummaryTitle });

    await page.getByRole('button', { name: 'Subscribe' }).click();
    await expect(summary).toBeVisible();
    await expect.poll(() => focusedText(page)).toContain(MESSAGES.form.errorSummaryTitle);
    await expect(summary.getByRole('link')).toHaveText(['Name is required.', 'Email is required.']);
    await expect(page.getByRole('textbox', { name: 'Name' })).toHaveAccessibleDescription(
      'Name is required.',
    );

    await page.getByLabel('Name').fill('Ann');
    await page.getByLabel('Email').fill('ann.example.com');
    await page.getByRole('button', { name: 'Subscribe' }).click();
    await expect(summary.getByRole('link')).toHaveText([MESSAGES.form.email]);
    await expect(page.getByLabel('Email')).toHaveAttribute('aria-invalid', 'true');

    // The summary entry takes the keyboard user to the field.
    await summary.getByRole('link', { name: MESSAGES.form.email }).click();
    await expect(page.getByLabel('Email')).toBeFocused();
    expect(backend.requests).toEqual([]);
  });

  test('shows the server validation errors on the fields', async ({ page, backend }) => {
    await backend.onPost(
      EMAIL_PATH,
      validationProblem([{ field: 'email', message: 'This email address is not accepted.' }]),
    );
    await backend.open('/');

    await page.getByLabel('Name').fill('Ann');
    await page.getByLabel('Email').fill('ann@example.com');
    await page.getByRole('button', { name: 'Subscribe' }).click();

    const summary = page.getByRole('alert').filter({ hasText: MESSAGES.form.errorSummaryTitle });
    await expect(summary.getByRole('link')).toHaveText(['This email address is not accepted.']);
    await expect.poll(() => focusedText(page)).toContain(MESSAGES.form.errorSummaryTitle);
    await expect(page.getByRole('textbox', { name: 'Email' })).toHaveAccessibleDescription(
      /This email address is not accepted\./,
    );
    await expect(page.getByLabel('Email')).toHaveValue('ann@example.com');
  });

  test('shows a network error and keeps the input', async ({ page, backend }) => {
    await backend.onPost(EMAIL_PATH, 'network-error');
    await backend.open('/');

    await page.getByLabel('Name').fill('Ann');
    await page.getByLabel('Email').fill('ann@example.com');
    await page.getByRole('button', { name: 'Subscribe' }).click();

    await expect(
      page.getByRole('alert').filter({ hasText: MESSAGES.apiError.network }),
    ).toBeVisible();
    await expect(page.getByLabel('Name')).toHaveValue('Ann');
    await expect(page.getByLabel('Email')).toHaveValue('ann@example.com');
    expect(backend.requests).toHaveLength(1);
  });

  test('works with the keyboard only on a 320 px wide screen', async ({ page, backend }) => {
    await page.setViewportSize({ width: 320, height: 640 });
    await backend.onPost(EMAIL_PATH, ACCEPTED);
    await backend.open('/');

    // No horizontal scrolling at 320 px (WCAG 1.4.10 reflow, NFR-12).
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow).toBeLessThanOrEqual(0);

    await page.getByLabel('Name').focus();
    await page.keyboard.type('Ann Example');
    await page.keyboard.press('Tab');
    await expect(page.getByLabel('Email')).toBeFocused();
    await page.keyboard.type('ann@example.com');
    await page.keyboard.press('Enter');

    await expect(
      page.getByRole('status').filter({ hasText: MESSAGES.signup.emailConfirmation }),
    ).toBeVisible();
    expect(backend.requests).toHaveLength(1);
  });
});
