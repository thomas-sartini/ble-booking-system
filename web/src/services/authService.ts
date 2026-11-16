import { apiRequest } from './apiClient'
import { tokenStore } from './tokenStore'
import type { Role } from './types'

export interface User {
    id: number
    email: string
    username: string
    firstName: string
    lastName: string
    role: Role
}

export interface LoginResponse {
    accessToken: string
    tokenType: string
    expiresIn: number
    refreshToken: string
    user: User
}

export const ROLES: readonly Role[] = ['STUDENT', 'STAFF', 'ADMIN']

export function isValidUser(value: unknown): value is User {
    const u = value as Partial<User> | null
    return !!u && typeof u.id === 'number' && ROLES.includes(u.role as Role)
}

export const authService = {
    async login(identifier: string, password: string): Promise<User> {
        const data = await apiRequest<LoginResponse>('/auth/login', {
            method: 'POST',
            body: { identifier, password },
        })
        tokenStore.setTokens(data.accessToken, data.refreshToken)
        tokenStore.setUserJson(JSON.stringify(data.user))
        return data.user
    },

    /** Best effort on the server, always clears local state. */
    async logout(): Promise<void> {
        const refreshToken = tokenStore.getRefreshToken()
        try {
            if (refreshToken) {
                await apiRequest<void>('/auth/logout', { method: 'POST', body: { refreshToken } })
            }
        } catch {
            // ignore: local state is cleared below either way
        } finally {
            tokenStore.clear()
        }
    },
}
