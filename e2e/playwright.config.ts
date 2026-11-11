import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: '.',
  timeout: 30000,
  workers: 1,
  use: {
    baseURL: 'http://localhost:8081',
  },
  projects: [
    { name: 'e2e', testIgnore: 'screenshots.test.ts' },
    { name: 'demo-data', testMatch: 'demo-data.setup.ts' },
    { name: 'screenshots', testMatch: 'screenshots.test.ts', dependencies: ['demo-data'] },
  ],
});
