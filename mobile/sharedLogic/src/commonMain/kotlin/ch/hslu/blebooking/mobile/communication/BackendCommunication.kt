
package ch.hslu.blebooking.mobile.communication

import ch.hslu.blebooking.mobile.communication.dto.request.LoginRequestDto
import ch.hslu.blebooking.mobile.communication.dto.request.RefreshTokenRequestDto
import ch.hslu.blebooking.mobile.communication.networking.ApiClient
import ch.hslu.blebooking.mobile.communication.networking.ApiResult
import ch.hslu.blebooking.mobile.model.AccessConfirmation
import ch.hslu.blebooking.mobile.model.Booking
import ch.hslu.blebooking.mobile.model.ResourceAccessRequest
import ch.hslu.blebooking.mobile.model.ResourceAccessResponse
import ch.hslu.blebooking.mobile.model.error.MobileCoreErrorCode
import ch.hslu.blebooking.mobile.model.error.MobileCoreException
import ch.hslu.blebooking.mobile.token.TokenStorage

/**
 * Concrete implementation of [CommunicationGateway].
 *
 * Handles communication with the REST backend, including
 * authentication, booking, and resource-access operations.
 *
 * Uses [TokenStorage] to manage authentication tokens
 * and support access token renewal.
 *
 * HTTP requests, response handling, and error mapping
 * will be implemented here.
 */
internal class BackendCommunication(
    private val tokenStorage: TokenStorage,
    private val apiClient: ApiClient
) : CommunicationGateway {

    override suspend fun login(
        identifier: String,
        password: String
    ) {
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

    override suspend fun logout() {
        TODO("Not yet implemented")
    }

    override suspend fun getActiveBookings(): List<Booking> {
        TODO("Not yet implemented")
    }

    override suspend fun requestAccess(
        request: ResourceAccessRequest
    ): ResourceAccessResponse {
        TODO("Not yet implemented")
    }

    override suspend fun confirmAccess(
        confirmation: AccessConfirmation
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun checkOut(
        resourceId: String
    ) {
        TODO("Not yet implemented")
    }

    /**
     * Requests a new access token from the backend
     * using a valid refresh token.
     *
     * @param request The refresh token request containing
     * the credentials required for token renewal.
     * @throws MobileCoreException If the refresh token is
     * invalid, expired, or a communication error occurs.
     */
    internal suspend fun refreshAccessToken(
        request: RefreshTokenRequestDto
    ) {
        TODO("Not yet implemented")
    }

    private fun ApiResult.Error.toMobileCoreException(): MobileCoreException {
        val code = if (status == null) {
            MobileCoreErrorCode.NETWORK_ERROR
        } else {
            MobileCoreErrorCode.fromHttpStatus(status.value)
        }
        return MobileCoreException(code = code, message = this.message)
    }
}
