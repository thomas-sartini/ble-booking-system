import { http, HttpResponse } from 'msw'
import type { BookingScope, DayAvailability, DayName, Slot, SlotStatus } from '../services/types'
import { contractAvailability, loginResponse, resourceDetails, resourceSummaries } from './fixtures'
import { BOOKING_HORIZON_DAYS, MAX_DURATION_MINUTES } from '../utils/booking'
import { addDays } from '../utils/week'
import {
  blockingBookings, cancelStored, createStored, findOverlap, getMine, listMine, toDto,
} from './bookingStore'
import { toZoneIso, zoneOffset } from './time'

const BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080'
const SLOT_MINUTES = 30
const MAX_RANGE_DAYS = 14

export function problem(status: number, code: string, detail: string, instance: string) {
  return HttpResponse.json(
    { type: 'about:blank', title: String(status), status, detail, instance, code },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
}

const unauthorized = (instance: string) => problem(401, 'UNAUTHORIZED', 'Authentication required.', instance)

const DAY_NAMES: DayName[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

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
  const horizon = now + BOOKING_HORIZON_DAYS * 86_400_000
  const blocking = blockingBookings(resourceId)
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
        const endMs = startMs + SLOT_MINUTES * 60_000
        let status: SlotStatus = 'AVAILABLE'
        if (startMs <= now) status = 'PAST'
        else {
          const blocker = blocking.find((b) => b.startMs < endMs && b.endMs > startMs)
          if (blocker) status = blocker.owner === 'me' ? 'BOOKED_BY_ME' : 'BOOKED'
        }
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

  http.post(`${BASE}/bookings`, async ({ request }) => {
    if (!request.headers.get('Authorization')) return unauthorized('/bookings')
    const bad = (detail: string) => problem(400, 'VALIDATION_ERROR', detail, '/bookings')
    const body = (await request.json().catch(() => null)) as
      { resourceId?: unknown; start?: unknown; end?: unknown } | null
    const startMs = typeof body?.start === 'string' ? Date.parse(body.start) : NaN
    const endMs = typeof body?.end === 'string' ? Date.parse(body.end) : NaN
    if (typeof body?.resourceId !== 'number' || isNaN(startMs) || isNaN(endMs)) {
      return bad('resourceId, start and end are required.')
    }
    if (endMs <= startMs) return bad('end must be after start.')

    const resource = resourceDetails.find((r) => r.id === body.resourceId)
    const zone = resource?.timeZone ?? 'Europe/Zurich'
    const startLocal = toZoneIso(startMs, zone)
    const endLocal = toZoneIso(endMs, zone)
    const startMin = toMinutes(startLocal.slice(11, 16))
    const endMin = toMinutes(endLocal.slice(11, 16))
    const minutes = (endMs - startMs) / 60_000
    if (startLocal.slice(17, 19) !== '00' || startMin % SLOT_MINUTES !== 0 || endMin % SLOT_MINUTES !== 0) {
      return bad('start and end must lie on the slot grid.')
    }
    if (minutes > MAX_DURATION_MINUTES) return bad('The booking is too long.')
    const now = Date.now()
    if (startMs <= now) return bad('start must be in the future.')
    if (startMs > now + BOOKING_HORIZON_DAYS * 86_400_000) return bad('start is beyond the booking horizon.')

    if (!resource) return problem(404, 'NOT_FOUND', 'Resource not found.', '/bookings')
    const sameDay = startLocal.slice(0, 10) === toZoneIso(endMs - 1, zone).slice(0, 10)
    const dayName = DAY_NAMES[(new Date(`${startLocal.slice(0, 10)}T00:00:00Z`).getUTCDay() + 6) % 7]
    const inWindow = sameDay && resource.windows.some(
      (w) => w.dayOfWeek === dayName && toMinutes(w.start) <= startMin && endMin <= toMinutes(w.end),
    )
    if (!inWindow) {
      return problem(409, 'OUTSIDE_AVAILABILITY', 'The interval is not inside a bookable window.', '/bookings')
    }
    if (findOverlap(resource.id, startMs, endMs)) {
      return problem(409, 'BOOKING_OVERLAP', 'The resource is already booked at this time.', '/bookings')
    }

    const stored = createStored(resource.id, startMs, endMs)
    return HttpResponse.json(toDto(stored), { status: 201, headers: { Location: `/bookings/${stored.id}` } })
  }),

  http.get(`${BASE}/bookings`, ({ request }) => {
    if (!request.headers.get('Authorization')) return unauthorized('/bookings')
    const scope = new URL(request.url).searchParams.get('scope') ?? 'all'
    if (scope !== 'upcoming' && scope !== 'past' && scope !== 'all') {
      return problem(400, 'VALIDATION_ERROR', 'scope must be upcoming, past or all.', '/bookings')
    }
    return HttpResponse.json(listMine(scope as BookingScope).map(toDto))
  }),

  http.get(`${BASE}/bookings/:id`, ({ request, params }) => {
    const instance = `/bookings/${params.id}`
    if (!request.headers.get('Authorization')) return unauthorized(instance)
    const booking = getMine(Number(params.id))
    if (!booking) return problem(404, 'NOT_FOUND', 'Booking not found.', instance)
    return HttpResponse.json(toDto(booking))
  }),

  http.delete(`${BASE}/bookings/:id`, ({ request, params }) => {
    const instance = `/bookings/${params.id}`
    if (!request.headers.get('Authorization')) return unauthorized(instance)
    const booking = getMine(Number(params.id))
    if (!booking) return problem(404, 'NOT_FOUND', 'Booking not found.', instance)
    if (cancelStored(booking) === 'invalid') {
      return problem(409, 'INVALID_STATUS', 'The booking can no longer be cancelled.', instance)
    }
    return new HttpResponse(null, { status: 204 })
  }),
]
