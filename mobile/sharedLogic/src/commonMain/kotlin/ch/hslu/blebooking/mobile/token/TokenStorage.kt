package ch.hslu.blebooking.mobile.token

internal interface TokenStorage {

    fun getAccessToken(): String?

    fun getRefreshToken(): String?

    fun storeTokens(
        accessToken: String,
        refreshToken: String
    )

    fun clearTokens()
}

