import { test as base, expect } from '@playwright/test';
import { MockBackend } from './mock-backend';

/**
 * The Playwright `test` with a mocked Core per test. After each test it checks that the UI called
 * no API path the test did not mock.
 */
export const test = base.extend<{ backend: MockBackend }>({
  backend: async ({ page }, use) => {
    const backend = new MockBackend(page);
    await backend.install();
    await use(backend);
    expect(backend.unexpected, 'API calls without a mock').toEqual([]);
  },
});

export { expect };
