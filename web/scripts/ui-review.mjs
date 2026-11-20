// Writes review screenshots to web/ui-review/ (git-ignored): node scripts/ui-review.mjs <prefix> [dark]
// With "dark" the dark theme is forced and the files are named <prefix>-dark-*.
// Starts the Vite dev server with the mock backend (VITE_USE_MOCKS=true), so no backend is needed and
// the screenshots show mock data only. The session is seeded into localStorage: no password is typed.
import { chromium } from '@playwright/test'
import { spawn } from 'node:child_process'
import { mkdirSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const dark = process.argv[3] === 'dark'
const prefix = `${process.argv[2] ?? 'after'}${dark ? '-dark' : ''}`
const PORT = 5199
const BASE = `http://localhost:${PORT}`
const outDir = fileURLToPath(new URL('../ui-review/', import.meta.url))
mkdirSync(outDir, { recursive: true })

const SIZES = { desktop: { width: 1440, height: 900 }, mobile: { width: 375, height: 812 } }
const USER = { id: 2, email: 'staff@example.com', username: 'staff', firstName: 'Sam', lastName: 'Staff', role: 'STAFF' }

const server = spawn('npx', ['vite', '--port', String(PORT), '--strictPort'], {
  cwd: fileURLToPath(new URL('..', import.meta.url)),
  env: { ...process.env, VITE_USE_MOCKS: 'true', VITE_API_URL: 'http://localhost:8080' },
  stdio: 'ignore',
})

async function waitForServer() {
  for (let i = 0; i < 100; i++) {
    try {
      if ((await fetch(BASE)).ok) return
    } catch { /* not up yet */ }
    await new Promise((r) => setTimeout(r, 200))
  }
  throw new Error('Vite dev server did not start')
}

async function seed(page) {
  await page.goto(`${BASE}/login`)
  await page.evaluate((user) => {
    localStorage.setItem('authToken', 'access-1')
    localStorage.setItem('refreshToken', 'refresh-1')
    localStorage.setItem('authUser', JSON.stringify(user))
  }, USER)
}

const settle = (page) => page.waitForTimeout(400)

try {
  await waitForServer()
  const browser = await chromium.launch()
  for (const [name, viewport] of Object.entries(SIZES)) {
    const context = await browser.newContext({ viewport })
    await context.addInitScript((theme) => localStorage.setItem('theme', theme), dark ? 'dark' : 'light')
    const page = await context.newPage()
    const shot = async (label, options = {}) => {
      await settle(page)
      await page.screenshot({ path: `${outDir}${prefix}-${name}-${label}.png`, fullPage: true, ...options })
    }

    await page.goto(`${BASE}/login`)
    await page.getByRole('button', { name: 'Sign In' }).waitFor()
    await shot('1-signin')

    await seed(page)
    await page.goto(`${BASE}/resources`)
    await page.getByRole('link', { name: /Cargo Bike 1/ }).first().waitFor()
    await shot('2-catalog')

    await page.goto(`${BASE}/resources/2`)
    // Narrow screens show one day at a time: move to the next weekday so enough cells are free.
    await page.getByRole('list', { name: 'Legend' }).waitFor()
    const dayTabs = page.getByRole('tablist', { name: 'Choose day' })
    if (await dayTabs.isVisible()) {
      let index = (new Date().getDay() + 6) % 7 + 1
      if (index > 4) {
        await page.getByRole('button', { name: /Next week/ }).click()
        index = 0
      }
      await dayTabs.getByRole('tab').nth(index).click()
    }
    const buttons = page.getByRole('button', { name: /Available/ })
    await buttons.nth(5).waitFor()
    // Take the elements once: a selected cell stops matching /Available/ and would shift a lazy locator.
    const cells = await buttons.elementHandles()
    // A real click first (checks nothing overlaps the cell); if the layout covers it, dispatch the event instead.
    for (const index of [4, 5]) {
      await cells[index].click({ timeout: 3000 }).catch(() => cells[index].dispatchEvent('click'))
    }
    await shot('3-details-selection')

    await page.getByRole('button', { name: 'Book…' }).click()
    await page.getByRole('dialog').waitFor()
    await shot('4-confirm-dialog', { fullPage: false })
    await page.getByRole('button', { name: 'Confirm booking' }).click()
    const toast = page.locator('[data-sonner-toast]').filter({ hasText: 'Booked Project Room' })
    await toast.waitFor()
    console.log(`${name}: toast role =`, await toast.getAttribute('role'))
    await shot('6-booked-toast', { fullPage: false })

    await page.goto(`${BASE}/bookings`)
    await page.getByText('Project Room D2.14').first().waitFor()
    await shot('5-my-bookings')
    await context.close()
  }
  await browser.close()
  console.log(`Screenshots written to ${outDir}`)
} finally {
  server.kill()
}
