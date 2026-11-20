import { render } from '@testing-library/react'
import { vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AppRoutes } from '../App'
import { AuthProvider } from '../context/AuthContext'
import { mockUser } from '../mocks/fixtures'
import { tokenStore } from '../services/tokenStore'

export const API = 'http://localhost:8080'

/** Puts a valid session into storage, as if the user had logged in earlier. */
export function signIn(user = mockUser) {
  tokenStore.setTokens('access-1', 'refresh-1')
  tokenStore.setUserJson(JSON.stringify(user))
}

export function renderApp(route: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[route]}>
        <AppRoutes />
      </MemoryRouter>
    </AuthProvider>,
  )
}

/** Monday 2026-11-16, 10:00 in Zurich. */
export const FIXED_NOW = '2026-11-16T10:00:00+01:00'

/**
 * Fixes "now" for slot status, horizon and upcoming/past. Only Date is faked, so timers,
 * fetch and MSW keep working. setup.ts restores real timers after each test.
 */
export function setNow(iso: string = FIXED_NOW) {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(iso))
}
