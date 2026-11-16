import { http, HttpResponse } from 'msw'
import type { DayAvailability, DayName, Slot, SlotStatus } from '../services/types'
import { contractAvailability, loginResponse, resourceDetails, resourceSummaries } from './fixtures'
import { addDays } from '../utils/week'

const BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080'
const SLOT_MINUTES = 30
const HORIZON_DAYS = 30
const MAX_RANGE_DAYS = 14

export function problem(status: number, code: string, detail: string, instance: string) {
  return HttpResponse.json(
    { type: 'about:blank', title: String(status), status, detail, instance, code },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
}

const unauthorized = (instance: string) => problem(401, 'UNAUTHORIZED', 'Authentication required.', instance)

const DAY_NAMES: DayName[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

/** Zurich UTC offset ("+01:00") on the given local date, DST aware. */
function zoneOffset(date: string, timeZone: string): string {
  const parts = new Intl.DateTimeFormat('en-US', { timeZone, timeZoneName: 'longOffset' })
    .formatToParts(new Date(`${date}T12:00:00Z`))
  const name = parts.find((p) => p.type === 'timeZoneName')?.value ?? 'GMT'
  return name === 'GMT' ? '+00:00' : name.replace('GMT', '')
}

function hash(text: string): number {
  let h = 0
  for (const ch of text) h = (h * 31 + ch.charCodeAt(0)) >>> 0
  return h
}

function minutesToTime(total: number): string {
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
}

function toMinutes(time: string): number {
  const [h, m] = time.split(':').map(Number)
  return h * 60 + m
}

function daysBetween(from: string, to: string): number {
  return Math.round((Date.parse(to) - Date.parse(from)) / 86_400_000)
}

function buildDays(resourceId: number, from: string, to: string): DayAvailability[] {
  const resource = resourceDetails.find((r) => r.id === resourceId)!
  const now = Date.now()
  const horizon = now + HORIZON_DAYS * 86_400_000
  const days: DayAvailability[] = []
  for (let date = from; date <= to; date = addDays(date, 1)) {
    const dayName = DAY_NAMES[(new Date(`${date}T00:00:00Z`).getUTCDay() + 6) % 7]
    const offset = zoneOffset(date, resource.timeZone)
    const slots: Slot[] = []
    for (const w of resource.windows.filter((w) => w.dayOfWeek === dayName)) {
      for (let m = toMinutes(w.start); m + SLOT_MINUTES <= toMinutes(w.end); m += SLOT_MINUTES) {
        const start = `${date}T${minutesToTime(m)}:00${offset}`
        const end = `${date}T${minutesToTime(m + SLOT_MINUTES)}:00${offset}`
        const startMs = Date.parse(start)
        if (startMs > horizon) continue
        const h = hash(`${resourceId}|${start}`)
        let status: SlotStatus = 'AVAILABLE'
        if (startMs <= now) status = 'PAST'
        else if (h % 7 === 0) status = 'BOOKED'
        else if (h % 11 === 0) status = 'BOOKED_BY_ME'
        slots.push({ start, end, status })
      }
    }
    days.push({ date, slots })
  }
  return days
}

export const handlers = [
  http.post(`${BASE}/auth/login`, async ({ request }) => {
    const body = (await request.json()) as { identifier?: string; password?: string }
    if (!body.identifier || !body.password) {
      return problem(400, 'VALIDATION_ERROR', 'identifier and password are required.', '/auth/login')
    }
    return HttpResponse.json(loginResponse)
  }),

  http.post(`${BASE}/auth/refresh`, () =>
    HttpResponse.json({
      accessToken: 'access-refreshed',
      tokenType: 'Bearer',
      expiresIn: 900,
      refreshToken: 'refresh-refreshed',
    }),
  ),

  http.post(`${BASE}/auth/logout`, () => new HttpResponse(null, { status: 204 })),

  http.get(`${BASE}/resources`, ({ request }) => {
    if (!request.headers.get('Authorization')) return unauthorized('/resources')
    const url = new URL(request.url)
    const type = url.searchParams.get('type')
    const q = url.searchParams.get('q')?.toLowerCase()
    if (type && type !== 'MOBILE' && type !== 'FIXED') {
      return problem(400, 'VALIDATION_ERROR', 'type must be MOBILE or FIXED.', '/resources')
    }
    const result = resourceSummaries.filter(
      (r) =>
        (!type || r.type === type) &&
        (!q || [r.name, r.description, r.position].some((f) => f?.toLowerCase().includes(q))),
    )
    return HttpResponse.json(result)
  }),

  http.get(`${BASE}/resources/:id`, ({ request, params }) => {
    if (!request.headers.get('Authorization')) return unauthorized(`/resources/${params.id}`)
    const resource = resourceDetails.find((r) => String(r.id) === params.id)
    if (!resource) return problem(404, 'NOT_FOUND', 'Resource not found.', `/resources/${params.id}`)
    return HttpResponse.json(resource)
  }),

  http.get(`${BASE}/resources/:id/availability`, ({ request, params }) => {
    const instance = `/resources/${params.id}/availability`
    if (!request.headers.get('Authorization')) return unauthorized(instance)
    const resource = resourceDetails.find((r) => String(r.id) === params.id)
    if (!resource) return problem(404, 'NOT_FOUND', 'Resource not found.', instance)

    const url = new URL(request.url)
    const from = url.searchParams.get('from')
    const to = url.searchParams.get('to')
    const isDate = (v: string | null): v is string => !!v && /^\d{4}-\d{2}-\d{2}$/.test(v) && !isNaN(Date.parse(v))
    if (!isDate(from) || !isDate(to)) {
      return problem(400, 'VALIDATION_ERROR', 'from and to are required (yyyy-MM-dd).', instance)
    }
    if (from > to) return problem(400, 'VALIDATION_ERROR', 'from must not be after to.', instance)
    if (daysBetween(from, to) + 1 > MAX_RANGE_DAYS) {
      return problem(400, 'VALIDATION_ERROR', `The range must not exceed ${MAX_RANGE_DAYS} days.`, instance)
    }

    if (resource.id === contractAvailability.resourceId && from === contractAvailability.from && to === contractAvailability.to) {
      return HttpResponse.json(contractAvailability)
    }
    return HttpResponse.json({
      resourceId: resource.id,
      timeZone: resource.timeZone,
      slotMinutes: SLOT_MINUTES,
      from,
      to,
      days: buildDays(resource.id, from, to),
    })
  }),
]
