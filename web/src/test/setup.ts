import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll, vi } from 'vitest'
import { resetBookings } from '../mocks/bookingStore'
import { server } from '../mocks/server'

beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  cleanup()
  vi.useRealTimers()
  resetBookings()
  server.resetHandlers()
  localStorage.clear()
})
afterAll(() => server.close())
