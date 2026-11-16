import { render } from '@testing-library/react'
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
