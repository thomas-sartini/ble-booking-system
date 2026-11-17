import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { rmSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { defineConfig, type Plugin } from 'vitest/config'

// public/mockServiceWorker.js is only for the dev mocks. Vite copies public/ into dist/, so remove it there:
// a production site must never register a mock service worker.
const excludeMockWorker = (): Plugin => ({
  name: 'exclude-mock-service-worker',
  apply: 'build',
  closeBundle() {
    rmSync(fileURLToPath(new URL('./dist/mockServiceWorker.js', import.meta.url)), { force: true })
  },
})

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss(), excludeMockWorker()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    // e2e/ belongs to Playwright (npm run e2e)
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
