import { setupWorker } from 'msw/browser'
import { seedDemoBookings } from './bookingStore'
import { handlers } from './handlers'

export const worker = setupWorker(...handlers)

seedDemoBookings()
