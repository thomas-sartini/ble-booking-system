import { ApiError } from './apiClient'

export const GENERIC_ERROR_MESSAGE = 'Something went wrong. Please try again.'

/** User-facing text per error code (booking-api.md §7). Never branches on `detail` text. */
export function messageForError(err: unknown): string {
  if (!(err instanceof ApiError)) return GENERIC_ERROR_MESSAGE
  switch (err.code) {
    case 'BOOKING_OVERLAP':
      return 'Someone just booked this time. Please choose another slot.'
    case 'OUTSIDE_AVAILABILITY':
      return 'This time is not bookable for you.'
    case 'INVALID_STATUS':
      return 'This booking can no longer be cancelled.'
    case 'NOT_FOUND':
      return 'This resource or booking is not available.'
    case 'FORBIDDEN':
      return 'Your account is not active. Please contact an administrator.'
    case 'VALIDATION_ERROR':
      return err.detail ?? GENERIC_ERROR_MESSAGE
    default:
      return GENERIC_ERROR_MESSAGE
  }
}
