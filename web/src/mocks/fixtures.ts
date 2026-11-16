import type { Availability, ResourceDetails, ResourceSummary, WeeklyWindow } from '../services/types'
import type { LoginResponse, User } from '../services/authService'

// Mirrors the dev seed in docs/api/booking-api.md §5. "Old Beamer" is INACTIVE and never returned.
const weekdays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'] as const
const daily = (start: string, end: string): WeeklyWindow[] =>
  weekdays.map((dayOfWeek) => ({ dayOfWeek, start, end }))

export const resourceDetails: ResourceDetails[] = [
  {
    id: 1,
    name: 'Cargo Bike 1',
    description: 'Electric cargo bike for transporting equipment between campuses.',
    type: 'MOBILE',
    position: 'Bike shed, Building A',
    pricePerHour: '5.00',
    timeZone: 'Europe/Zurich',
    windows: daily('08:00', '18:00'),
  },
  {
    id: 2,
    name: 'Project Room D2.14',
    description: 'Project room with a whiteboard and a large display.',
    type: 'FIXED',
    position: 'Building D, Room 2.14',
    pricePerHour: '0.00',
    timeZone: 'Europe/Zurich',
    windows: [...daily('07:00', '21:00'), { dayOfWeek: 'SATURDAY', start: '09:00', end: '14:00' }],
  },
  {
    id: 3,
    name: '3D Printer Lab',
    description: 'Lab with several 3D printers. Staff only.',
    type: 'FIXED',
    position: 'Building B, Room 0.05',
    pricePerHour: '12.50',
    timeZone: 'Europe/Zurich',
    windows: daily('09:00', '17:00'),
  },
]

export const resourceSummaries: ResourceSummary[] = resourceDetails.map(
  ({ id, name, description, type, position, pricePerHour }) => ({
    id, name, description, type, position, pricePerHour,
  }),
)

/** Example response from booking-api.md §4, verbatim. */
export const contractAvailability: Availability = {
  resourceId: 3,
  timeZone: 'Europe/Zurich',
  slotMinutes: 30,
  from: '2026-11-17',
  to: '2026-11-17',
  days: [
    {
      date: '2026-11-17',
      slots: [
        { start: '2026-11-17T08:00:00+01:00', end: '2026-11-17T08:30:00+01:00', status: 'PAST' },
        { start: '2026-11-17T08:30:00+01:00', end: '2026-11-17T09:00:00+01:00', status: 'BOOKED' },
        { start: '2026-11-17T09:00:00+01:00', end: '2026-11-17T09:30:00+01:00', status: 'AVAILABLE' },
      ],
    },
  ],
}

export const mockUser: User = {
  id: 2,
  email: 'staff@example.com',
  username: 'staff',
  firstName: 'Sam',
  lastName: 'Staff',
  role: 'STAFF',
}

export const loginResponse: LoginResponse = {
  accessToken: 'access-1',
  tokenType: 'Bearer',
  expiresIn: 900,
  refreshToken: 'refresh-1',
  user: mockUser,
}
