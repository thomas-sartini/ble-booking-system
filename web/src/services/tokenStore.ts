const ACCESS_KEY = 'authToken'
const REFRESH_KEY = 'refreshToken'
const USER_KEY = 'authUser'

type SessionExpiredHandler = () => void
let sessionExpiredHandler: SessionExpiredHandler | null = null

export const tokenStore = {
  getAccessToken: (): string | null => localStorage.getItem(ACCESS_KEY),
  getRefreshToken: (): string | null => localStorage.getItem(REFRESH_KEY),
  getUserJson: (): string | null => localStorage.getItem(USER_KEY),

  setTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem(ACCESS_KEY, accessToken)
    localStorage.setItem(REFRESH_KEY, refreshToken)
  },
  setUserJson(json: string): void {
    localStorage.setItem(USER_KEY, json)
  },
  clear(): void {
    localStorage.removeItem(ACCESS_KEY)
    localStorage.removeItem(REFRESH_KEY)
    localStorage.removeItem(USER_KEY)
  },

  /** Registered by AuthContext so apiClient can log the user out without importing it. */
  onSessionExpired(handler: SessionExpiredHandler | null): void {
    sessionExpiredHandler = handler
  },
  notifySessionExpired(): void {
    sessionExpiredHandler?.()
  },
}
