import type { DayAvailability, Slot } from '../services/types'

/**
 * Maximum booking duration: 4 hours (booking-api.md D7).
 * The API does not expose it, so the grid, the dialog and the mocks share this one constant.
 * The server stays the authority and answers 400 VALIDATION_ERROR beyond it.
 */
export const MAX_DURATION_MINUTES = 4 * 60

/**
 * Booking horizon: a booking may start at most 30 days ahead (booking-api.md D8, backend config
 * `app.booking.horizon`). The backend returns no slots beyond it, so the week navigation stops there.
 */
export const BOOKING_HORIZON_DAYS = 30

export const MAX_DURATION_MESSAGE = `A booking can last at most ${MAX_DURATION_MINUTES / 60} hours.`
export const NOT_AVAILABLE_MESSAGE = 'The selection must only contain available, consecutive slots.'

/** A contiguous range of slots on one day. `anchor` is the first click; `complete` after the second click. */
export interface Selection {
  date: string
  anchor: number
  from: number
  to: number
  complete: boolean
}

export type PickResult = { selection: Selection } | { selection: Selection | null; error: string }

/**
 * Applies a click on slot `index` of `day`.
 * 1st click: selects that slot. 2nd click on the same day: selects the range up to it.
 * A range is rejected (previous selection kept) if it crosses a slot that is not AVAILABLE or
 * not directly adjacent, or exceeds the maximum duration. Any further click starts a new selection.
 */
export function pickSlot(
  prev: Selection | null,
  day: DayAvailability,
  index: number,
  slotMinutes: number,
): PickResult {
  const clicked = day.slots[index]
  if (!clicked || clicked.status !== 'AVAILABLE') {
    return { selection: prev, error: NOT_AVAILABLE_MESSAGE }
  }
  if (!prev || prev.complete || prev.date !== day.date) {
    return { selection: { date: day.date, anchor: index, from: index, to: index, complete: false } }
  }
  const from = Math.min(prev.anchor, index)
  const to = Math.max(prev.anchor, index)
  const range = day.slots.slice(from, to + 1)
  const consecutive = range.every(
    (s, i) => s.status === 'AVAILABLE' && (i === 0 || range[i - 1].end === s.start),
  )
  if (!consecutive) return { selection: prev, error: NOT_AVAILABLE_MESSAGE }
  if (range.length * slotMinutes > MAX_DURATION_MINUTES) return { selection: prev, error: MAX_DURATION_MESSAGE }
  return { selection: { date: day.date, anchor: prev.anchor, from, to, complete: true } }
}

export function selectedSlots(day: DayAvailability | undefined, selection: Selection | null): Slot[] {
  return day && selection ? day.slots.slice(selection.from, selection.to + 1) : []
}

/** "1 h 30 min", "2 h", "30 min" */
export function formatDuration(minutes: number): string {
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return [h ? `${h} h` : '', m ? `${m} min` : ''].filter(Boolean).join(' ')
}
