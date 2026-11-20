import { defineConfig, devices } from '@playwright/test'

// End-to-end tests against a RUNNING backend (dev profile, http://localhost:8080) and its database.
// Not part of `npm test` and not run in CI. See docs/verification/us-02-web-booking.md.
// Port 5173 is fixed because the backend CORS config allows only 5173/5174.
export default defineConfig({
  testDir: './e2e',
  // One shared database: tests run one after the other.
  workers: 1,
  fullyParallel: false,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:5173',
    // Traces record request bodies, including the login password. Only keep them for failures,
    // and test-results/ and playwright-report/ are git-ignored.
    trace: 'retain-on-failure',
    screenshot: 'off',
    video: 'off',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm run dev -- --port 5173 --strictPort',
    url: 'http://localhost:5173',
    reuseExistingServer: false,
    env: { VITE_API_URL: 'http://localhost:8080', VITE_USE_MOCKS: 'false' },
  },
})
