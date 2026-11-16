import { expect, test as base, type APIRequestContext, type Locator, type Page } from '@playwright/test'
import { mkdirSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

export const API = 'http://localhost:8080'

export type UserKey = 'student' | 'staff'
const EMAIL: Record<UserKey, string> = { student: 'student@example.com', staff: 'staff@example.com' }

export interface Session {
  accessToken: string
  refreshToken: string
  user: Record<string, unknown>
}

// ---- credentials ----------------------------------------------------------------------------
// DEV_USER_PASSWORD comes from the environment or from an .env file (E2E_ENV_FILE, default: repo root).
// It is never logged or written anywhere.
let cachedPassword: string | undefined
function devPassword(): string {
  if (cachedPassword) return cachedPassword
  let value = process.env.DEV_USER_PASSWORD
  if (!value) {
    const file = process.env.E2E_ENV_FILE ?? fileURLToPath(new URL('../../.env', import.meta.url))
    try {
      const line = readFileSync(file, 'utf8').split('\n').find((l) => l.startsWith('DEV_USER_PASSWORD='))
      value = line?.slice('DEV_USER_PASSWORD='.length).trim()
    } catch {
      // handled below
    }
  }
  if (!value) throw new Error('DEV_USER_PASSWORD not found: set it or point E2E_ENV_FILE to the .env file.')
  cachedPassword = value
  return value
}

export const credentials = (user: UserKey) => ({ email: EMAIL[user], password: devPassword() })

// ---- dates ----------------------------------------------------------------------------------
const ZONE = 'Europe/Zurich'
const utc = (date: string) => new Date(`${date}T00:00:00Z`)
export const addDays = (date: string, days: number) =>
  new Date(utc(date).getTime() + days * 86_400_000).toISOString().slice(0, 10)
export const todayZurich = () =>
  new Intl.DateTimeFormat('en-CA', { timeZone: ZONE, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
const mondayOf = (date: string) => addDays(date, -((utc(date).getUTCDay() + 6) % 7))

/** Matches the app's long date label ("Tue, 17 Nov 2026") without depending on the ICU version of the browser. */
export const dateLabel = (date: string) => new RegExp(`\\b${Number(date.slice(8))}\\s+\\w+\\.?\\s+${date.slice(0, 4)}`)

// ---- API access (setup, free-slot search, cleanup) -------------------------------------------
export async function apiLogin(request: APIRequestContext, user: UserKey): Promise<Session> {
  const { email, password } = credentials(user)
  const response = await request.post(`${API}/auth/login`, { data: { identifier: email, password } })
  expect(response.status(), 'API login').toBe(200)
  const body = await response.json()
  return { accessToken: body.accessToken, refreshToken: body.refreshToken, user: body.user }
}

const auth = (s: Session) => ({ Authorization: `Bearer ${s.accessToken}` })

export interface FreeRange {
  date: string
  /** "HH:mm" of the first and the last slot of the range */
  first: string
  last: string
  /** "HH:mm" of every slot in the range */
  times: string[]
}

/**
 * Looks for `count` consecutive AVAILABLE slots within the next 7 days (starting tomorrow, because
 * today's remaining slots may be PAST). Searches each day from the evening backwards so manual use
 * of the dev data is unlikely to collide.
 */
export async function findFreeRange(
  request: APIRequestContext, session: Session, resourceId: number, count: number, skip = 0,
): Promise<FreeRange> {
  const from = addDays(todayZurich(), 1)
  const to = addDays(from, 6)
  const response = await request.get(`${API}/resources/${resourceId}/availability`, {
    headers: auth(session), params: { from, to },
  })
  expect(response.status(), 'availability').toBe(200)
  const { days } = (await response.json()) as {
    days: { date: string; slots: { start: string; end: string; status: string }[] }[]
  }
  let seen = 0
  for (const day of days) {
    for (let i = day.slots.length - count; i >= 0; i--) {
      const run = day.slots.slice(i, i + count)
      const free = run.every((s, j) => s.status === 'AVAILABLE' && (j === 0 || run[j - 1].end === s.start))
      if (free && seen++ >= skip) {
        const times = run.map((s) => s.start.slice(11, 16))
        return { date: day.date, first: times[0], last: times[times.length - 1], times }
      }
    }
  }
  throw new Error(`No ${count} free consecutive slots for resource ${resourceId} between ${from} and ${to}`)
}

export async function listBookings(request: APIRequestContext, session: Session, scope: string) {
  const response = await request.get(`${API}/bookings`, { headers: auth(session), params: { scope } })
  expect(response.status()).toBe(200)
  return (await response.json()) as { id: number; status: string; resource: { id: number }; start: string; end: string }[]
}

// ---- UI helpers -----------------------------------------------------------------------------
/** Puts a session into localStorage as the app would after login (the login form itself is tested once). */
export async function seedSession(page: Page, session: Session, path = '/resources') {
  await page.goto('/login')
  await page.evaluate((s) => {
    localStorage.setItem('authToken', s.accessToken)
    localStorage.setItem('refreshToken', s.refreshToken)
    localStorage.setItem('authUser', JSON.stringify(s.user))
  }, session)
  await page.goto(path)
}

/** Goes to the week that contains `date` and returns the day's region. */
export async function showDay(page: Page, date: string): Promise<Locator> {
  const weeks = (utc(mondayOf(date)).getTime() - utc(mondayOf(todayZurich())).getTime()) / (7 * 86_400_000)
  for (let i = 0; i < weeks; i++) await page.getByRole('button', { name: /Next week/ }).click()
  const region = page.getByRole('region', { name: date })
  await expect(region).toBeVisible()
  return region
}

export const slotButton = (region: Locator, time: string) =>
  region.getByRole('button', { name: new RegExp(`^${time}–`) })

/** Clicks the first and the last slot of a range. */
export async function selectRange(region: Locator, range: FreeRange) {
  await slotButton(region, range.first).click()
  if (range.last !== range.first) await slotButton(region, range.last).click()
}

/** Writes a screenshot for the verification report, only if E2E_SCREENSHOTS=1. Never call it on the login form. */
export async function shot(page: Page, name: string) {
  if (process.env.E2E_SCREENSHOTS !== '1') return
  const dir = fileURLToPath(new URL('../../docs/verification/assets/', import.meta.url))
  mkdirSync(dir, { recursive: true })
  await page.screenshot({ path: `${dir}${name}.png`, fullPage: true })
}

// ---- test fixture: remembers bookings created through the UI and cancels them afterwards ---------
interface Tracker {
  track(page: Page, user: UserKey): void
  ids: { user: UserKey; id: number }[]
}

export const test = base.extend<{ tracker: Tracker }>({
  tracker: async ({ request }, provide) => {
    const ids: Tracker['ids'] = []
    const tracker: Tracker = {
      ids,
      track(page, user) {
        page.on('response', async (response) => {
          const req = response.request()
          if (req.method() === 'POST' && new URL(response.url()).pathname === '/bookings' && response.status() === 201) {
            ids.push({ user, id: (await response.json()).id })
          }
        })
      },
    }
    try {
      await provide(tracker)
    } finally {
      // Runs also when the test failed. DELETE is idempotent (204 if already cancelled).
      for (const { user, id } of ids) {
        const session = await apiLogin(request, user)
        const response = await request.delete(`${API}/bookings/${id}`, { headers: auth(session) })
        if (![204, 404].includes(response.status())) console.warn(`cleanup of booking ${id} answered ${response.status()}`)
      }
    }
  },
})

export { expect }
