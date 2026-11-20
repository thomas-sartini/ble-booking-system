import { beforeEach, describe, expect, it } from 'vitest'
import { ApiError, apiRequest } from '../services/apiClient'
import type { Availability, Booking } from '../services/types'
import { setNow, signIn } from '../test/utils'
import { addForeignBooking, lockBooking, seedBooking } from './bookingStore'

// "now" is Monday 2026-11-16 10:00 Zurich (setNow default). Cargo Bike 1 (id 1): Mon-Fri 08:00-18:00, CHF 5.00/h.
const RES = 1
const slot = (date: string, from: string, to: string) => ({
  resourceId: RES, start: `${date}T${from}:00+01:00`, end: `${date}T${to}:00+01:00`,
})
const post = (body: unknown) => apiRequest<Booking>('/bookings', { method: 'POST', body })
const code = async (p: Promise<unknown>) => {
  const err = await p.catch((e) => e)
  expect(err).toBeInstanceOf(ApiError)
  return `${(err as ApiError).status} ${(err as ApiError).code}`
}

beforeEach(() => {
  setNow()
  signIn()
})

describe('POST /bookings (mock)', () => {
  it('creates a booking with locked price and Zurich times', async () => {
    const booking = await post(slot('2026-11-17', '10:00', '11:30'))
    expect(booking).toMatchObject({
      id: 1, status: 'BOOKED', price: '7.50', currency: 'CHF', cancellable: true,
      start: '2026-11-17T10:00:00+01:00', end: '2026-11-17T11:30:00+01:00',
      resource: { id: 1, name: 'Cargo Bike 1' },
    })
  })

  it('accepts a Z timestamp and answers in Zurich time', async () => {
    const booking = await post({ resourceId: RES, start: '2026-11-17T09:00:00Z', end: '2026-11-17T09:30:00Z' })
    expect(booking.start).toBe('2026-11-17T10:00:00+01:00')
  })

  it('validates in contract order: 400 before 404 before 409', async () => {
    expect(await code(post({ resourceId: RES }))).toBe('400 VALIDATION_ERROR')
    expect(await code(post(slot('2026-11-17', '11:00', '10:00')))).toBe('400 VALIDATION_ERROR')
    expect(await code(post(slot('2026-11-17', '10:15', '10:45')))).toBe('400 VALIDATION_ERROR') // off grid
    expect(await code(post(slot('2026-11-17', '08:00', '12:30')))).toBe('400 VALIDATION_ERROR') // 4.5 h
    expect(await code(post(slot('2026-11-16', '09:00', '09:30')))).toBe('400 VALIDATION_ERROR') // past
    expect(await code(post(slot('2026-12-17', '10:00', '10:30')))).toBe('400 VALIDATION_ERROR') // > 30 days
    expect(await code(post({ ...slot('2026-11-17', '10:00', '10:30'), resourceId: 99 }))).toBe('404 NOT_FOUND')
    expect(await code(post(slot('2026-11-17', '07:00', '07:30')))).toBe('409 OUTSIDE_AVAILABILITY')
    expect(await code(post(slot('2026-11-21', '10:00', '10:30')))).toBe('409 OUTSIDE_AVAILABILITY') // Saturday
  })

  it('accepts exactly 4 hours', async () => {
    await expect(post(slot('2026-11-17', '08:00', '12:00'))).resolves.toMatchObject({ price: '20.00' })
  })

  it('answers BOOKING_OVERLAP for any blocking booking but allows adjacent ones', async () => {
    addForeignBooking(RES, '2026-11-17T10:00:00+01:00', '2026-11-17T11:00:00+01:00')
    expect(await code(post(slot('2026-11-17', '10:30', '11:30')))).toBe('409 BOOKING_OVERLAP')
    await expect(post(slot('2026-11-17', '11:00', '11:30'))).resolves.toMatchObject({ status: 'BOOKED' })
    await expect(post(slot('2026-11-17', '09:30', '10:00'))).resolves.toMatchObject({ status: 'BOOKED' })
  })

  it('frees the slot again after a cancellation', async () => {
    const { id } = await post(slot('2026-11-17', '10:00', '10:30'))
    await apiRequest(`/bookings/${id}`, { method: 'DELETE' })
    await expect(post(slot('2026-11-17', '10:00', '10:30'))).resolves.toMatchObject({ id: 2 })
  })

  it('requires a token', async () => {
    localStorage.clear()
    expect(await code(post(slot('2026-11-17', '10:00', '10:30')))).toBe('401 UNAUTHORIZED')
  })
})

describe('GET and DELETE /bookings (mock)', () => {
  it('lists upcoming ascending and past descending, only own bookings', async () => {
    const late = seedBooking(slot('2026-11-19', '10:00', '11:00'))
    const early = seedBooking(slot('2026-11-17', '10:00', '11:00'))
    const old = seedBooking({ ...slot('2026-11-02', '10:00', '11:00'), status: 'COMPLETED' })
    const older = seedBooking({ ...slot('2026-10-26', '10:00', '11:00'), status: 'NO_SHOW' })
    const cancelled = seedBooking({ ...slot('2026-11-18', '10:00', '11:00'), status: 'CANCELLED' })
    addForeignBooking(RES, '2026-11-20T10:00:00+01:00', '2026-11-20T11:00:00+01:00')

    const ids = async (scope: string) =>
      (await apiRequest<Booking[]>('/bookings', { query: { scope } })).map((b) => b.id)
    expect(await ids('upcoming')).toEqual([early, late])
    expect(await ids('past')).toEqual([cancelled, old, older])
    expect(await ids('all')).toEqual([early, late, cancelled, old, older])
    expect(await code(ids('bogus'))).toBe('400 VALIDATION_ERROR')
  })

  it('gets one booking and hides bookings of others', async () => {
    const id = seedBooking(slot('2026-11-17', '10:00', '11:00'))
    await expect(apiRequest<Booking>(`/bookings/${id}`)).resolves.toMatchObject({ id, price: '5.00' })
    addForeignBooking(RES, '2026-11-20T10:00:00+01:00', '2026-11-20T11:00:00+01:00')
    expect(await code(apiRequest('/bookings/2'))).toBe('404 NOT_FOUND')
    expect(await code(apiRequest('/bookings/2', { method: 'DELETE' }))).toBe('404 NOT_FOUND')
  })

  it('cancels with 204 and is idempotent', async () => {
    const id = seedBooking(slot('2026-11-17', '10:00', '11:00'))
    await expect(apiRequest(`/bookings/${id}`, { method: 'DELETE' })).resolves.toBeUndefined()
    await expect(apiRequest(`/bookings/${id}`, { method: 'DELETE' })).resolves.toBeUndefined()
    expect((await apiRequest<Booking>(`/bookings/${id}`)).status).toBe('CANCELLED')
  })

  it('answers INVALID_STATUS for checked-in, started and locked bookings', async () => {
    const checkedIn = seedBooking({ ...slot('2026-11-17', '10:00', '11:00'), status: 'CHECKED_IN' })
    const started = seedBooking(slot('2026-11-16', '09:30', '11:00'))
    const locked = seedBooking(slot('2026-11-18', '10:00', '11:00'))
    lockBooking(locked)
    expect((await apiRequest<Booking>(`/bookings/${locked}`)).cancellable).toBe(true)
    for (const id of [checkedIn, started, locked]) {
      expect(await code(apiRequest(`/bookings/${id}`, { method: 'DELETE' }))).toBe('409 INVALID_STATUS')
    }
  })
})

describe('availability (mock) follows the booking store', () => {
  const availability = () =>
    apiRequest<Availability>(`/resources/${RES}/availability`, { query: { from: '2026-11-16', to: '2026-11-17' } })
  const statusAt = (a: Availability, date: string, time: string) =>
    a.days.find((d) => d.date === date)!.slots.find((s) => s.start.slice(11, 16) === time)!.status

  it('marks own bookings BOOKED_BY_ME, others BOOKED, started slots PAST', async () => {
    seedBooking(slot('2026-11-17', '10:00', '11:00'))
    addForeignBooking(RES, '2026-11-17T12:00:00+01:00', '2026-11-17T12:30:00+01:00')
    seedBooking(slot('2026-11-16', '09:00', '10:00'))
    const a = await availability()
    expect(statusAt(a, '2026-11-17', '10:00')).toBe('BOOKED_BY_ME')
    expect(statusAt(a, '2026-11-17', '10:30')).toBe('BOOKED_BY_ME')
    expect(statusAt(a, '2026-11-17', '11:00')).toBe('AVAILABLE')
    expect(statusAt(a, '2026-11-17', '12:00')).toBe('BOOKED')
    expect(statusAt(a, '2026-11-16', '09:30')).toBe('PAST')
    expect(statusAt(a, '2026-11-16', '10:30')).toBe('AVAILABLE')
  })

  it('does not block with cancelled bookings and drops slots beyond the horizon', async () => {
    seedBooking({ ...slot('2026-11-17', '10:00', '11:00'), status: 'CANCELLED' })
    expect(statusAt(await availability(), '2026-11-17', '10:00')).toBe('AVAILABLE')
    const far = await apiRequest<Availability>(`/resources/${RES}/availability`, {
      query: { from: '2026-12-14', to: '2026-12-20' },
    })
    expect(far.days.find((d) => d.date === '2026-12-14')!.slots.length).toBeGreaterThan(0) // day 28
    expect(far.days.find((d) => d.date === '2026-12-18')!.slots).toEqual([]) // day 32
  })
})
