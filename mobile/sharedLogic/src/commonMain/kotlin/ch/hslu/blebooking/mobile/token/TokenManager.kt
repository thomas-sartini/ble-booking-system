package ch.hslu.blebooking.mobile.token

import ch.hslu.blebooking.mobile.api.SecureStorage
import ch.hslu.blebooking.mobile.model.error.MobileCoreErrorCode
import ch.hslu.blebooking.mobile.model.error.MobileCoreException

internal class TokenManager(
    private val secureStorage: SecureStorage
) : TokenStorage {
    private var accessToken: String? = null

    override fun getAccessToken(): String? {
        return accessToken
    }

    override fun getRefreshToken(): String? {
        return try {
            secureStorage.get("refreshToken")
        } catch (e: Exception) {
            throw MobileCoreException(
                code = MobileCoreErrorCode.STORAGE_ERROR,
                message = "Failed to retrieve refresh token from secure storage",
                cause = e
            )
        }
    }

    override fun storeTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        try {
            secureStorage.save("refreshToken", refreshToken)
        } catch (e: Exception) {
            throw MobileCoreException(
                code = MobileCoreErrorCode.STORAGE_ERROR,
                message = "Failed to save refresh token to secure storage",
                cause = e
            )
        }
    }

    override fun clearTokens() {
        this.accessToken = null
        try {
            secureStorage.remove("refreshToken")
        } catch (e: Exception) {
            throw MobileCoreException(
                code = MobileCoreErrorCode.STORAGE_ERROR,
                message = "Failed to remove refresh token from secure storage",
                cause = e
            )
        }
    }
}
