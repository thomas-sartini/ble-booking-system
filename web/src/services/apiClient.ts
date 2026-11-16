import { tokenStore } from './tokenStore'

export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly detail: string | null

  constructor(status: number, code: string, detail: string | null) {
    super(detail ?? code)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.detail = detail
  }
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  query?: Record<string, string | number | undefined | null>
  body?: unknown
  signal?: AbortSignal
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const url = new URL(path, API_BASE_URL)
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value))
    }
  }
  return url.toString()
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const problem = await response.json()
    const code = typeof problem?.code === 'string' ? problem.code : response.status === 401 ? 'UNAUTHORIZED' : 'UNKNOWN'
    const detail = typeof problem?.detail === 'string' ? problem.detail : null
    return new ApiError(response.status, code, detail)
  } catch {
    return new ApiError(response.status, response.status === 401 ? 'UNAUTHORIZED' : 'UNKNOWN', null)
  }
}

async function send(path: string, options: RequestOptions, withAuth: boolean): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  const token = tokenStore.getAccessToken()
  if (withAuth && token) headers.Authorization = `Bearer ${token}`
  try {
    return await fetch(buildUrl(path, options.query), {
      method: options.method ?? 'GET',
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
      signal: options.signal,
    })
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') throw err
    throw new ApiError(0, 'NETWORK_ERROR', null)
  }
}

// One refresh at a time: parallel 401s await the same promise.
let refreshInFlight: Promise<void> | null = null

function refreshSession(): Promise<void> {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      const refreshToken = tokenStore.getRefreshToken()
      if (!refreshToken) throw new ApiError(401, 'UNAUTHORIZED', null)
      const response = await send('/auth/refresh', { method: 'POST', body: { refreshToken } }, false)
      if (!response.ok) throw await toApiError(response)
      const data = await response.json()
      tokenStore.setTokens(data.accessToken, data.refreshToken)
    })().finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}

async function parse<T>(response: Response): Promise<T> {
  if (response.status === 204) return undefined as T
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

/** Calls the backend with the bearer token. Refreshes once on 401 and replays the request. */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const isAuthCall = path.startsWith('/auth/')
  let response = await send(path, options, !isAuthCall)

  if (response.status === 401 && !isAuthCall) {
    try {
      await refreshSession()
    } catch {
      tokenStore.clear()
      tokenStore.notifySessionExpired()
      throw new ApiError(401, 'UNAUTHORIZED', null)
    }
    response = await send(path, options, true)
  }

  if (!response.ok) throw await toApiError(response)
  return parse<T>(response)
}
