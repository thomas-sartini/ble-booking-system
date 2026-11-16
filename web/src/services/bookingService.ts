import { apiRequest } from './apiClient'
import type { Booking, BookingScope, CreateBookingRequest } from './types'

// Typed for booking-api.md §4. Used from step 2 (create) and step 3 (list/cancel).
export const bookingService = {
  createBooking(request: CreateBookingRequest) {
    return apiRequest<Booking>('/bookings', { method: 'POST', body: request })
  },
  listBookings(scope: BookingScope = 'all', signal?: AbortSignal) {
    return apiRequest<Booking[]>('/bookings', { query: { scope }, signal })
  },
  getBooking(id: number | string, signal?: AbortSignal) {
    return apiRequest<Booking>(`/bookings/${id}`, { signal })
  },
  cancelBooking(id: number | string) {
    return apiRequest<void>(`/bookings/${id}`, { method: 'DELETE' })
  },
}
