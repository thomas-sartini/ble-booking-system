package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.communication.dto.response.UserProfileResponseDto
import ch.hslu.blebooking.mobile.communication.networking.ApiClient
import ch.hslu.blebooking.mobile.communication.networking.ApiResult
import ch.hslu.blebooking.mobile.model.Role
import ch.hslu.blebooking.mobile.model.UserProfile
import ch.hslu.blebooking.mobile.model.error.MobileCoreErrorCode
import ch.hslu.blebooking.mobile.model.error.MobileCoreException

internal class UserService(
    private val apiClient: ApiClient
) {
    suspend fun getUserProfile(): UserProfile {
        return when (val result = apiClient.getUserProfile()) {
            is ApiResult.Success -> result.data.toUserProfile()
            is ApiResult.Error -> throw result.toMobileCoreException()
        }
    }

    private fun UserProfileResponseDto.toUserProfile(): UserProfile {
        val mappedRole = try {
            Role.valueOf(role.trim().uppercase())
        } catch (_: IllegalArgumentException) {
            throw MobileCoreException(
                code = MobileCoreErrorCode.UNKNOWN_ERROR,
                message = "Invalid or unknown user role: $role"
            )
        }

        return UserProfile(
            firstName = firstName,
            lastName = lastName,
            username = username,
            email = email,
            role = mappedRole
        )
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
        return MobileCoreException(
            code = code,
            message = message.takeIf { it.isNotBlank() } ?: code.message,
            cause = cause,
        )
    }
}

