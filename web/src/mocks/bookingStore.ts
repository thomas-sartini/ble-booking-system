import type { Booking, BookingStatus } from '../services/types'
import { priceFor } from '../utils/price'
import { addDays, todayIn } from '../utils/week'
import { resourceDetails } from './fixtures'
import { toZoneIso, zoneOffset } from './time'

// In-memory bookings of the mock backend. Tests reset it after every test (src/test/setup.ts).
// The mock user (fixtures.ts) is the only "me"; bookings of `other` only block slots.

const BLOCKING: BookingStatus[] = ['BOOKED', 'CHECKED_IN']

interface StoredBooking {
  id: number
  resourceId: number
  startMs: number
  endMs: number
  status: BookingStatus
  owner: 'me' | 'other'
  createdMs: number
  /** Server refuses cancellation although the client was told it is cancellable. */
  locked: boolean
}

let bookings: StoredBooking[] = []
let nextId = 1

export function resetBookings() {
  bookings = []
  nextId = 1
}

export const isBlocking = (b: { status: BookingStatus }) => BLOCKING.includes(b.status)

export function findOverlap(resourceId: number, startMs: number, endMs: number) {
  return bookings.find((b) => b.resourceId === resourceId && isBlocking(b) && b.startMs < endMs && b.endMs > startMs)
}

export function blockingBookings(resourceId: number) {
  return bookings.filter((b) => b.resourceId === resourceId && isBlocking(b))
}

export function createStored(
  resourceId: number, startMs: number, endMs: number,
  options: { status?: BookingStatus; owner?: 'me' | 'other' } = {},
): StoredBooking {
  const booking: StoredBooking = {
    id: nextId++, resourceId, startMs, endMs,
    status: options.status ?? 'BOOKED', owner: options.owner ?? 'me',
    createdMs: Date.now(), locked: false,
  }
  bookings.push(booking)
  return booking
}

export function getMine(id: number) {
  return bookings.find((b) => b.id === id && b.owner === 'me')
}

export function listMine(scope: 'upcoming' | 'past' | 'all'): StoredBooking[] {
  const now = Date.now()
  const mine = bookings.filter((b) => b.owner === 'me')
  const upcoming = mine.filter((b) => b.endMs > now && isBlocking(b)).sort((a, b) => a.startMs - b.startMs)
  const past = mine.filter((b) => !(b.endMs > now && isBlocking(b))).sort((a, b) => b.startMs - a.startMs)
  return scope === 'upcoming' ? upcoming : scope === 'past' ? past : [...upcoming, ...past]
}

export function toDto(b: StoredBooking): Booking {
  const resource = resourceDetails.find((r) => r.id === b.resourceId)!
  const minutes = (b.endMs - b.startMs) / 60_000
  return {
    id: b.id,
    resource: { id: resource.id, name: resource.name, type: resource.type, position: resource.position },
    start: toZoneIso(b.startMs, resource.timeZone),
    end: toZoneIso(b.endMs, resource.timeZone),
    status: b.status,
    price: priceFor(resource.pricePerHour, minutes),
    currency: 'CHF',
    // D12. `locked` is deliberately not part of this, see lockBooking().
    cancellable: b.status === 'BOOKED' && b.startMs > Date.now(),
    createdAt: toZoneIso(b.createdMs, resource.timeZone),
  }
}

export function cancelStored(b: StoredBooking): 'ok' | 'invalid' {
  if (b.status === 'CANCELLED') return 'ok'
  if (b.status !== 'BOOKED' || b.startMs <= Date.now() || b.locked) return 'invalid'
  b.status = 'CANCELLED'
  return 'ok'
}

// ---- Test and dev helpers -------------------------------------------------------------------

interface SeedOptions {
  resourceId: number
  /** ISO-8601 with offset, e.g. 2026-11-18T09:00:00+01:00 */
  start: string
  end: string
  status?: BookingStatus
}

/** Adds a booking of the mock user. Returns its id. */
export function seedBooking({ resourceId, start, end, status }: SeedOptions): number {
  return createStored(resourceId, Date.parse(start), Date.parse(end), { status }).id
}

/** Adds a booking of someone else. Seed it after a page loaded to provoke 409 BOOKING_OVERLAP. */
export function addForeignBooking(resourceId: number, start: string, end: string): void {
  createStored(resourceId, Date.parse(start), Date.parse(end), { owner: 'other' })
}

/**
 * From now on DELETE answers 409 INVALID_STATUS for this booking, while GET still reports
 * `cancellable: true`. This is the "check-in happened after the list was loaded" case.
 */
export function lockBooking(id: number): void {
  const b = bookings.find((x) => x.id === id)
  if (b) b.locked = true
}

/** A few bookings for `VITE_USE_MOCKS=true npm run dev`. */
export function seedDemoBookings(now = new Date()): void {
  const zone = 'Europe/Zurich'
  const isWeekend = (d: string) => [0, 6].includes(new Date(`${d}T00:00:00Z`).getUTCDay())
  const weekday = (from: string, step: number) => {
    let d = from
    while (isWeekend(d)) d = addDays(d, step)
    return d
  }
  const iso = (date: string, time: string) => `${date}T${time}:00${zoneOffset(date, zone)}`
  const today = todayIn(zone, now)
  const next = weekday(addDays(today, 1), 1)
  const last = weekday(addDays(today, -2), -1)
  seedBooking({ resourceId: 2, start: iso(next, '10:00'), end: iso(next, '11:30') })
  seedBooking({ resourceId: 1, start: iso(last, '09:00'), end: iso(last, '10:00'), status: 'COMPLETED' })
  addForeignBooking(2, iso(next, '14:00'), iso(next, '16:00'))
}
