import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests of the public pages against a mocked backend (FE-21). The dev server serves
 * the UI; every `/api` call is answered by Playwright request interception (`e2e/support`), so no
 * Core is needed. Tests against the running stack follow in FE-26.
 *
 * A separate port keeps these tests apart from a dev server on 4200 that talks to a real Core.
 * Locally a server already running on that port is reused; on CI (`CI` set) a fresh one starts.
 */
const port = 4201;
const baseURL = `http://localhost:${port}`;
const isCi = !!process.env['CI'];

export default defineConfig({
  testDir: 'e2e',
  testMatch: '**/*.e2e.ts',
  fullyParallel: true,
  forbidOnly: isCi,
  retries: isCi ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL,
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: `npx ng serve --port ${port}`,
    url: baseURL,
    reuseExistingServer: !isCi,
    timeout: 120_000,
  },
});
