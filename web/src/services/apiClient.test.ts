import { delay, http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { server } from '../mocks/server'
import { API } from '../test/utils'
import { ApiError, apiRequest } from './apiClient'
import { tokenStore } from './tokenStore'

function seedSession() {
  tokenStore.setTokens('old-access', 'old-refresh')
}

function refreshHandler(counter: { calls: number }, ok = true) {
  return http.post(`${API}/auth/refresh`, async () => {
    counter.calls++
    await delay(20)
    if (!ok) return HttpResponse.json({ code: 'UNAUTHORIZED' }, { status: 401 })
    return HttpResponse.json({ accessToken: 'new-access', tokenType: 'Bearer', expiresIn: 900, refreshToken: 'new-refresh' })
  })
}

const pingNeedsNewToken = http.get(`${API}/ping`, ({ request }) =>
  request.headers.get('Authorization') === 'Bearer new-access'
    ? HttpResponse.json({ ok: true })
    : new HttpResponse(null, { status: 401 }),
)

afterEach(() => {
  vi.restoreAllMocks()
  tokenStore.onSessionExpired(null)
})

describe('apiRequest', () => {
  it('sends the bearer token and no credentials option', async () => {
    seedSession()
    let auth: string | null = null
    server.use(http.get(`${API}/ping`, ({ request }) => {
      auth = request.headers.get('Authorization')
      return HttpResponse.json({ ok: true })
    }))
    const fetchSpy = vi.spyOn(globalThis, 'fetch')

    await apiRequest('/ping')

    expect(auth).toBe('Bearer old-access')
    const init = fetchSpy.mock.calls[0][1] as RequestInit
    expect(init).not.toHaveProperty('credentials')
  })

  it('refreshes once on 401, replays the request and rotates the tokens', async () => {
    seedSession()
    const refresh = { calls: 0 }
    server.use(pingNeedsNewToken, refreshHandler(refresh))

    await expect(apiRequest('/ping')).resolves.toEqual({ ok: true })

    expect(refresh.calls).toBe(1)
    expect(tokenStore.getAccessToken()).toBe('new-access')
    expect(tokenStore.getRefreshToken()).toBe('new-refresh')
  })

  it('shares one refresh between parallel 401s', async () => {
    seedSession()
    const refresh = { calls: 0 }
    server.use(pingNeedsNewToken, refreshHandler(refresh))

    const results = await Promise.all([apiRequest('/ping'), apiRequest('/ping'), apiRequest('/ping')])

    expect(results).toEqual([{ ok: true }, { ok: true }, { ok: true }])
    expect(refresh.calls).toBe(1)
  })

  it('clears the session, notifies and throws UNAUTHORIZED when the refresh fails', async () => {
    seedSession()
    tokenStore.setUserJson('{"id":1}')
    const expired = vi.fn()
    tokenStore.onSessionExpired(expired)
    const refresh = { calls: 0 }
    server.use(pingNeedsNewToken, refreshHandler(refresh, false))

    await expect(apiRequest('/ping')).rejects.toMatchObject({ status: 401, code: 'UNAUTHORIZED' })

    expect(expired).toHaveBeenCalledTimes(1)
    expect(tokenStore.getAccessToken()).toBeNull()
    expect(tokenStore.getRefreshToken()).toBeNull()
    expect(tokenStore.getUserJson()).toBeNull()
  })

  it('does not refresh again when the replayed request is 401 as well', async () => {
    seedSession()
    const refresh = { calls: 0 }
    server.use(
      http.get(`${API}/ping`, () => new HttpResponse(null, { status: 401 })),
      refreshHandler(refresh),
    )

    await expect(apiRequest('/ping')).rejects.toMatchObject({ status: 401, code: 'UNAUTHORIZED' })
    expect(refresh.calls).toBe(1)
  })

  it('does not try to refresh for /auth/* calls', async () => {
    const refresh = { calls: 0 }
    server.use(
      http.post(`${API}/auth/login`, () => HttpResponse.json({ code: 'UNAUTHORIZED', detail: 'bad' }, { status: 401 })),
      refreshHandler(refresh),
    )

    await expect(apiRequest('/auth/login', { method: 'POST', body: {} })).rejects.toMatchObject({ code: 'UNAUTHORIZED' })
    expect(refresh.calls).toBe(0)
  })

  it('maps Problem Details to ApiError', async () => {
    server.use(http.get(`${API}/ping`, () =>
      HttpResponse.json({ status: 409, detail: 'Taken.', code: 'BOOKING_OVERLAP' }, { status: 409 }),
    ))

    const err = await apiRequest('/ping').catch((e) => e)
    expect(err).toBeInstanceOf(ApiError)
    expect(err).toMatchObject({ status: 409, code: 'BOOKING_OVERLAP', detail: 'Taken.' })
  })

  it('falls back to UNKNOWN for a non-JSON error body', async () => {
    server.use(http.get(`${API}/ping`, () => HttpResponse.text('boom', { status: 500 })))
    await expect(apiRequest('/ping')).rejects.toMatchObject({ status: 500, code: 'UNKNOWN', detail: null })
  })

  it('maps a network failure to NETWORK_ERROR with status 0', async () => {
    server.use(http.get(`${API}/ping`, () => HttpResponse.error()))
    await expect(apiRequest('/ping')).rejects.toMatchObject({ status: 0, code: 'NETWORK_ERROR' })
  })

  it('handles 204 No Content', async () => {
    server.use(http.delete(`${API}/things/1`, () => new HttpResponse(null, { status: 204 })))
    await expect(apiRequest('/things/1', { method: 'DELETE' })).resolves.toBeUndefined()
  })

  it('sends query params and skips empty ones', async () => {
    let search = ''
    server.use(http.get(`${API}/ping`, ({ request }) => {
      search = new URL(request.url).search
      return HttpResponse.json({})
    }))
    await apiRequest('/ping', { query: { type: 'FIXED', q: '', x: undefined } })
    expect(search).toBe('?type=FIXED')
  })
})
