import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { loginResponse } from './mocks/fixtures'
import { server } from './mocks/server'
import { tokenStore } from './services/tokenStore'
import { API, renderApp, signIn } from './test/utils'

describe('login', () => {
  it('stores the real login response and lands on the catalog', async () => {
    renderApp('/login')
    await userEvent.type(screen.getByPlaceholderText('firstname.lastname@ble.edu'), 'staff@example.com')
    await userEvent.type(screen.getByPlaceholderText('••••••••••••'), 'secret-pw')
    await userEvent.click(screen.getByRole('button', { name: 'Sign In' }))

    expect(await screen.findByRole('heading', { name: 'Resources' })).toBeInTheDocument()
    expect(tokenStore.getAccessToken()).toBe(loginResponse.accessToken)
    expect(tokenStore.getRefreshToken()).toBe(loginResponse.refreshToken)
    expect(JSON.parse(tokenStore.getUserJson()!)).toEqual(loginResponse.user)
  })

  it('sends identifier and password', async () => {
    let body: unknown
    server.use(http.post(`${API}/auth/login`, async ({ request }) => {
      body = await request.json()
      return HttpResponse.json(loginResponse)
    }))
    renderApp('/login')
    await userEvent.type(screen.getByPlaceholderText('firstname.lastname@ble.edu'), 'staff@example.com')
    await userEvent.type(screen.getByPlaceholderText('••••••••••••'), 'secret-pw')
    await userEvent.click(screen.getByRole('button', { name: 'Sign In' }))
    await screen.findByRole('heading', { name: 'Resources' })
    expect(body).toEqual({ identifier: 'staff@example.com', password: 'secret-pw' })
  })

  it('shows the existing wording on a 401', async () => {
    server.use(http.post(`${API}/auth/login`, () => HttpResponse.json({ code: 'UNAUTHORIZED' }, { status: 401 })))
    renderApp('/login')
    await userEvent.type(screen.getByPlaceholderText('firstname.lastname@ble.edu'), 'a@b.ch')
    await userEvent.type(screen.getByPlaceholderText('••••••••••••'), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: 'Sign In' }))
    expect(await screen.findByText('Email or password is incorrect.')).toBeInTheDocument()
  })
})

describe('layout and routing', () => {
  it('shows the user name and nav links, and signs out', async () => {
    signIn()
    let logoutBody: unknown
    server.use(http.post(`${API}/auth/logout`, async ({ request }) => {
      logoutBody = await request.json()
      return new HttpResponse(null, { status: 204 })
    }))
    renderApp('/resources')

    expect(await screen.findByText('Sam Staff')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Resources' })).toHaveAttribute('href', '/resources')
    expect(screen.getByRole('link', { name: 'My bookings' })).toHaveAttribute('href', '/bookings')

    await userEvent.click(screen.getByRole('button', { name: 'Sign out' }))
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(logoutBody).toEqual({ refreshToken: 'refresh-1' })
    expect(tokenStore.getAccessToken()).toBeNull()
    expect(tokenStore.getRefreshToken()).toBeNull()
  })

  it('still signs out locally when the server call fails', async () => {
    signIn()
    server.use(http.post(`${API}/auth/logout`, () => HttpResponse.error()))
    renderApp('/resources')
    await userEvent.click(await screen.findByRole('button', { name: 'Sign out' }))
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(tokenStore.getAccessToken()).toBeNull()
  })

  it.each(['/', '/dashboard'])('redirects %s to /resources', async (route) => {
    signIn()
    renderApp(route)
    expect(await screen.findByRole('heading', { name: 'Resources' })).toBeInTheDocument()
  })

  it('redirects unauthenticated users to /login', async () => {
    renderApp('/resources')
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('discards a stale persisted user without numeric id or valid role', async () => {
    tokenStore.setTokens('a', 'r')
    tokenStore.setUserJson(JSON.stringify({ email: 'old@example.com', role: 'Student' }))
    renderApp('/resources')
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    expect(tokenStore.getAccessToken()).toBeNull()
  })

  it('logs out when the session expires during a request', async () => {
    signIn()
    server.use(
      http.get(`${API}/resources`, () => new HttpResponse(null, { status: 401 })),
      http.post(`${API}/auth/refresh`, () => HttpResponse.json({ code: 'UNAUTHORIZED' }, { status: 401 })),
    )
    renderApp('/resources')
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument()
    await waitFor(() => expect(tokenStore.getAccessToken()).toBeNull())
  })

  it('shows the bookings page on /bookings', async () => {
    signIn()
    renderApp('/bookings')
    expect(await screen.findByRole('heading', { name: 'My bookings' })).toBeInTheDocument()
    expect(await screen.findByText('No upcoming bookings')).toBeInTheDocument()
  })
})
