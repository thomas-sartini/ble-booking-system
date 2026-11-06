package ch.hslu.blebooking.mobile.token

import ch.hslu.blebooking.mobile.model.error.MobileCoreException

/**
 * Internal contract for managing authentication tokens within the Mobile Core.
 *
 * Responsibilities:
 * - Keep short-lived access credentials in memory only.
 * - Provide lifecycle management for stored tokens (retrieval, storage, and clearing upon logout or expiration).
 * - Throw [MobileCoreException] with [ch.hslu.blebooking.mobile.model.error.MobileCoreErrorCode.STORAGE_ERROR]
 *   if an unrecoverable storage failure occurs.
 */
internal interface TokenStorage {

    /**
     * Retrieves the current short-lived access token from in-memory cache.
     *
     * @return The active access token, or null if unauthenticated or expired.
     */
    fun getAccessToken(): String?

    /**
     * Retrieves the long-lived renewal credential (refresh token) from persistent secure storage.
     *
     * @return The stored refresh token, or null if no active session exists.
     * @throws MobileCoreException If reading from secure storage fails.
     */
    @Throws(MobileCoreException::class)
    fun getRefreshToken(): String?

    /**
     * Stores authentication credentials received from the backend.
     *
     * The [accessToken] must be retained in memory, whereas the [refreshToken]
     * must be persisted via secure storage.
     *
     * @param accessToken The short-lived bearer credential for authenticating API requests.
     * @param refreshToken The long-lived renewal credential used to acquire new access tokens.
     * @throws MobileCoreException If persisting to secure storage fails.
     */
    @Throws(MobileCoreException::class)
    fun storeTokens(
        accessToken: String,
        refreshToken: String
    )

    /**
     * Clears all authentication credentials, invalidating the local session.
     *
     * Clears the in-memory access token and purges the refresh token from secure storage.
     * @throws MobileCoreException If removing from secure storage fails.
     */
    @Throws(MobileCoreException::class)
    fun clearTokens()
}
