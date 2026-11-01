package ch.hslu.blebooking.mobile.token

import ch.hslu.blebooking.mobile.api.SecureStorage

internal class TokenManager(
    private val secureStorage: SecureStorage
) : TokenStorage{
    private var accessToken: String? = null

    override fun getAccessToken(): String? {
        TODO("Not yet implemented")
    }

    override fun getRefreshToken(): String? {
        TODO("Not yet implemented")
    }

    override fun storeTokens(accessToken: String, refreshToken: String) {
        secureStorage.save("accessToken", accessToken);
        secureStorage.save("refreshToken", refreshToken);
    }

    override fun clearTokens() {
        TODO("Not yet implemented")
    }
}
