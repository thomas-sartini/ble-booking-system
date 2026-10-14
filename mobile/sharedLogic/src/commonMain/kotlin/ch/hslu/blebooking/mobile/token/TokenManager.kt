package ch.hslu.blebooking.mobile.token

import ch.hslu.blebooking.mobile.api.SecureStorage

internal class TokenManager(
    private val secureStorage: SecureStorage
) {
    private var accessToken: String? = null

    fun getAccessToken(): String? {
        TODO("Not yet implemented")
    }

    fun getRefreshToken(): String? {
        TODO("Not yet implemented")
    }

    fun storeTokens(accessToken: String, refreshToken: String) {
        TODO("Not yet implemented")
    }

    fun clearTokens() {
        TODO("Not yet implemented")
    }
}
