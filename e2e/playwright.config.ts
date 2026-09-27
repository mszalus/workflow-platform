import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: '.',
  timeout: 30000,
  use: {
    baseURL: 'http://localhost:9080',
  },
  projects: [
    { name: 'e2e', testIgnore: 'screenshots.test.ts' },
    { name: 'screenshots', testMatch: 'screenshots.test.ts' },
  ],
});
