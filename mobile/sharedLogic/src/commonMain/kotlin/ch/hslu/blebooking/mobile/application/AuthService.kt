
package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.communication.dto.request.LoginRequestDto
import ch.hslu.blebooking.mobile.communication.dto.request.LogoutRequestDto
import ch.hslu.blebooking.mobile.communication.dto.request.RefreshTokenRequestDto
import ch.hslu.blebooking.mobile.communication.networking.ApiClient
import ch.hslu.blebooking.mobile.communication.networking.ApiResult
import ch.hslu.blebooking.mobile.model.error.MobileCoreErrorCode
import ch.hslu.blebooking.mobile.model.error.MobileCoreException
import ch.hslu.blebooking.mobile.token.TokenStorage

/** Coordinates authentication, token storage, and access token renewal. */
internal class AuthService(
    private val tokenStorage: TokenStorage,
    private val apiClient: ApiClient
) {

    suspend fun login(
        identifier: String,
        password: String
    ) {
        if(!identifier.isValidEmail()){
            throw MobileCoreException(MobileCoreErrorCode.VALIDATION_ERROR, "Invalid email")
        }
        val request = LoginRequestDto(
            identifier = identifier,
            password = password
        )

        when (val result = apiClient.login(request)) {

            is ApiResult.Success -> {
                val loginResponse = result.data

                tokenStorage.storeTokens(
                    loginResponse.accessToken,
                    loginResponse.refreshToken
                )
            }

            is ApiResult.Error -> {
                throw result.toMobileCoreException()
            }
        }

    }

    suspend fun logout() {
        val rt = tokenStorage.getRefreshToken()
        if(rt.isNullOrBlank()){
            tokenStorage.clearTokens()
            return
        }
        val request = LogoutRequestDto(
            refreshToken = rt
        )
        when (val result = apiClient.logout(request)) {
            is ApiResult.Success -> {
                tokenStorage.clearTokens()
            }
            is ApiResult.Error -> {
                throw result.toMobileCoreException()
            }
        }
    }

    /**
     * Requests a new access token from the backend
     * using a valid refresh token.
     *
     * the credentials required for token renewal.
     * @throws MobileCoreException If the refresh token is
     * invalid, expired, or a communication error occurs.
     */
    internal suspend fun refreshAccessToken() {
        val rt = tokenStorage.getRefreshToken()
        if(rt.isNullOrBlank()){
            tokenStorage.clearTokens()
            return
        }
        val request = RefreshTokenRequestDto(
            refreshToken = rt
        )
        when (val result = apiClient.refresh(request)) {
            is ApiResult.Success -> {
                tokenStorage.clearTokens()
                tokenStorage.storeTokens(
                    accessToken = result.data.accessToken,
                    refreshToken = result.data.refreshToken
                )
            }
            is ApiResult.Error -> {
                throw result.toMobileCoreException()
            }
        }
    }

    private fun ApiResult.Error.toMobileCoreException(): MobileCoreException {
        val code = when (this) {
            is ApiResult.HttpError -> if (status.value == 503) {
                MobileCoreErrorCode.SERVER_ERROR
            } else {
                MobileCoreErrorCode.fromHttpStatus(status.value)
            }
            is ApiResult.NetworkError, is ApiResult.Timeout -> MobileCoreErrorCode.NETWORK_ERROR
            is ApiResult.DecodingError, is ApiResult.UnexpectedError -> MobileCoreErrorCode.UNKNOWN_ERROR
        }
        return MobileCoreException(code = code, message = message, cause = cause)
    }

    private fun String.isValidEmail(): Boolean {
        if (this.isNullOrBlank()) return false
        val emailRegex = Regex(
            """^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"""
        )

        return emailRegex.matches(this)
    }
}
