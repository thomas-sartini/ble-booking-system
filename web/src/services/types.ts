// Shapes from docs/api/booking-api.md §3 (binding contract).

export type Role = 'STUDENT' | 'STAFF' | 'ADMIN'
export type ResourceType = 'MOBILE' | 'FIXED'
export type BookingStatus = 'BOOKED' | 'CHECKED_IN' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW'
export type SlotStatus = 'AVAILABLE' | 'BOOKED' | 'BOOKED_BY_ME' | 'PAST'
export type DayName =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY'

export interface ResourceSummary {
  id: number
  name: string
  description: string | null
  type: ResourceType
  position: string | null
  pricePerHour: string
}

export interface WeeklyWindow {
  dayOfWeek: DayName
  start: string
  end: string
}

export interface ResourceDetails extends ResourceSummary {
  timeZone: string
  windows: WeeklyWindow[]
}

export interface Slot {
  start: string
  end: string
  status: SlotStatus
}

export interface DayAvailability {
  date: string
  slots: Slot[]
}

export interface Availability {
  resourceId: number
  timeZone: string
  slotMinutes: number
  from: string
  to: string
  days: DayAvailability[]
}

export interface BookingResource {
  id: number
  name: string
  type: ResourceType
  position: string | null
}

export interface Booking {
  id: number
  resource: BookingResource
  start: string
  end: string
  status: BookingStatus
  price: string
  currency: 'CHF'
  cancellable: boolean
  createdAt: string
}

export interface CreateBookingRequest {
  resourceId: number
  start: string
  end: string
}

export type BookingScope = 'upcoming' | 'past' | 'all'
