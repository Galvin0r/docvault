import { defineConfig, devices } from '@playwright/test';

const port = Number(process.env.E2E_PORT ?? 4200);
const baseURL = process.env.E2E_BASE_URL ?? `http://127.0.0.1:${port}`;

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: process.env.CI ? [['html'], ['list']] : 'list',
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'off',
  },
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        command: 'node e2e/start-stack.mjs',
        url: baseURL,
        reuseExistingServer: false,
        timeout: 300_000,
      },
  projects: [
    {
      name: 'chrome',
      use: { ...devices['Desktop Chrome'], channel: 'chrome' },
    },
  ],
});
