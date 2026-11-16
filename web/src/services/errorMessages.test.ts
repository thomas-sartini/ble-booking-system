import { describe, expect, it } from 'vitest'
import { ApiError } from './apiClient'
import { GENERIC_ERROR_MESSAGE, messageForError } from './errorMessages'

describe('messageForError', () => {
  it.each([
    ['BOOKING_OVERLAP', 'Someone just booked this time. Please choose another slot.'],
    ['OUTSIDE_AVAILABILITY', 'This time is not bookable for you.'],
    ['INVALID_STATUS', 'This booking can no longer be cancelled.'],
    ['NOT_FOUND', 'This resource or booking is not available.'],
  ])('%s', (code, message) => {
    expect(messageForError(new ApiError(409, code, 'ignored detail text'))).toBe(message)
  })

  it('VALIDATION_ERROR shows the detail', () => {
    expect(messageForError(new ApiError(400, 'VALIDATION_ERROR', 'from must not be after to.'))).toBe(
      'from must not be after to.',
    )
  })

  it('VALIDATION_ERROR without detail falls back', () => {
    expect(messageForError(new ApiError(400, 'VALIDATION_ERROR', null))).toBe(GENERIC_ERROR_MESSAGE)
  })

  it('other codes, network errors and non-API errors use the generic message', () => {
    expect(messageForError(new ApiError(500, 'UNKNOWN', 'NOT_FOUND'))).toBe(GENERIC_ERROR_MESSAGE)
    expect(messageForError(new ApiError(0, 'NETWORK_ERROR', null))).toBe(GENERIC_ERROR_MESSAGE)
    expect(messageForError(new Error('x'))).toBe(GENERIC_ERROR_MESSAGE)
    expect(GENERIC_ERROR_MESSAGE).toBe('Something went wrong. Please try again.')
  })
})
