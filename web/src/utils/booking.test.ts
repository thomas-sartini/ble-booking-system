import { describe, expect, it } from 'vitest'
import type { DayAvailability, Slot, SlotStatus } from '../services/types'
import {
  formatDuration, MAX_DURATION_MESSAGE, MAX_DURATION_MINUTES, NOT_AVAILABLE_MESSAGE, pickSlot,
} from './booking'
import { priceFor } from './price'

const pad = (n: number) => String(n).padStart(2, '0')
const at = (i: number) => `2026-11-17T${pad(8 + Math.floor(i / 2))}:${i % 2 ? '30' : '00'}:00+01:00`

/** 30-minute slots from 08:00; `statuses` overrides single slots by index. */
function makeDay(count: number, statuses: Record<number, SlotStatus> = {}): DayAvailability {
  const slots: Slot[] = Array.from({ length: count }, (_, i) => ({
    start: at(i), end: at(i + 1), status: statuses[i] ?? 'AVAILABLE',
  }))
  return { date: '2026-11-17', slots }
}

describe('pickSlot', () => {
  it('first click selects one slot, second click selects the range', () => {
    const day = makeDay(10)
    const first = pickSlot(null, day, 2, 30)
    expect(first).toEqual({ selection: { date: day.date, anchor: 2, from: 2, to: 2, complete: false } })
    const second = pickSlot(first.selection, day, 5, 30)
    expect(second).toEqual({ selection: { date: day.date, anchor: 2, from: 2, to: 5, complete: true } })
  })

  it('accepts the second click before the first', () => {
    const day = makeDay(10)
    const first = pickSlot(null, day, 5, 30).selection!
    expect(pickSlot(first, day, 2, 30).selection).toMatchObject({ from: 2, to: 5, complete: true })
  })

  it('a click after a complete range starts a new selection', () => {
    const day = makeDay(10)
    const done = pickSlot(pickSlot(null, day, 1, 30).selection, day, 3, 30).selection!
    expect(pickSlot(done, day, 7, 30).selection).toMatchObject({ from: 7, to: 7, complete: false })
  })

  it('a click on another day starts a new selection there', () => {
    const day = makeDay(10)
    const other = { ...makeDay(10), date: '2026-11-18' }
    const first = pickSlot(null, day, 1, 30).selection!
    expect(pickSlot(first, other, 4, 30).selection).toMatchObject({ date: '2026-11-18', from: 4, to: 4 })
  })

  it.each<SlotStatus>(['BOOKED', 'BOOKED_BY_ME', 'PAST'])('rejects a range that crosses a %s slot', (status) => {
    const day = makeDay(10, { 4: status })
    const first = pickSlot(null, day, 2, 30).selection!
    const result = pickSlot(first, day, 6, 30)
    expect(result).toEqual({ selection: first, error: NOT_AVAILABLE_MESSAGE })
  })

  it('rejects a range across a gap between windows', () => {
    const day = makeDay(6)
    day.slots[3] = { ...day.slots[3], start: '2026-11-17T13:00:00+01:00', end: '2026-11-17T13:30:00+01:00' }
    const first = pickSlot(null, day, 2, 30).selection!
    expect(pickSlot(first, day, 3, 30)).toMatchObject({ error: NOT_AVAILABLE_MESSAGE })
  })

  it('accepts exactly the maximum duration and rejects one slot more', () => {
    const maxSlots = MAX_DURATION_MINUTES / 30
    const day = makeDay(maxSlots + 2)
    const first = pickSlot(null, day, 0, 30).selection!
    expect(pickSlot(first, day, maxSlots - 1, 30).selection).toMatchObject({ complete: true, to: maxSlots - 1 })
    expect(pickSlot(first, day, maxSlots, 30)).toEqual({ selection: first, error: MAX_DURATION_MESSAGE })
  })

  it('ignores a click on a slot that is not available', () => {
    const day = makeDay(3, { 1: 'BOOKED' })
    expect(pickSlot(null, day, 1, 30)).toEqual({ selection: null, error: NOT_AVAILABLE_MESSAGE })
  })

  it('keeps the maximum at 4 hours (D7)', () => {
    expect(MAX_DURATION_MINUTES).toBe(240)
    expect(MAX_DURATION_MESSAGE).toContain('4 hours')
  })
})

describe('formatDuration', () => {
  it('formats hours and minutes', () => {
    expect(formatDuration(30)).toBe('30 min')
    expect(formatDuration(120)).toBe('2 h')
    expect(formatDuration(90)).toBe('1 h 30 min')
  })
})

describe('priceFor (D11)', () => {
  it('multiplies price per hour by duration', () => {
    expect(priceFor('5.00', 90)).toBe('7.50')
    expect(priceFor('12.50', 120)).toBe('25.00')
    expect(priceFor('0.00', 240)).toBe('0.00')
  })

  it('rounds HALF_UP to 2 decimals', () => {
    expect(priceFor('0.05', 30)).toBe('0.03') // 0.025
    expect(priceFor('0.01', 30)).toBe('0.01') // 0.005
    expect(priceFor('0.01', 29)).toBe('0.00') // 0.00483
    expect(priceFor('2.00', 20)).toBe('0.67') // 0.6666
  })
})
