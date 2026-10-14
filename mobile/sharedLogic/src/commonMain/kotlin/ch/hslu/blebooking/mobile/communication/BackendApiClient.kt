package ch.hslu.blebooking.mobile.communication

import ch.hslu.blebooking.mobile.communication.dto.AccessConfirmationRequestDto
import ch.hslu.blebooking.mobile.communication.dto.BookingDto
import ch.hslu.blebooking.mobile.communication.dto.CheckOutRequestDto
import ch.hslu.blebooking.mobile.communication.dto.LoginRequestDto
import ch.hslu.blebooking.mobile.communication.dto.LoginResponseDto
import ch.hslu.blebooking.mobile.communication.dto.RefreshTokenRequestDto
import ch.hslu.blebooking.mobile.communication.dto.RefreshTokenResponseDto
import ch.hslu.blebooking.mobile.communication.dto.ResourceAccessRequestDto
import ch.hslu.blebooking.mobile.communication.dto.ResourceAccessResponseDto
import io.ktor.client.HttpClient

/**
 * Low-level REST client used by [BackendCommunication].
 *
 * Executes HTTP requests using transport DTOs.
 */
internal class BackendApiClient(
    private val httpClient: HttpClient
) {

    suspend fun login(request: LoginRequestDto): LoginResponseDto {
        TODO("Not yet implemented")
    }

    suspend fun refresh(request: RefreshTokenRequestDto): RefreshTokenResponseDto {
        TODO("Not yet implemented")
    }

    suspend fun logout(refreshToken: String) {
        TODO("Not yet implemented")
    }

    suspend fun getActiveBookings(token: String): List<BookingDto> {
        TODO("Not yet implemented")
    }

    suspend fun requestAccess(
        token: String,
        request: ResourceAccessRequestDto
    ): ResourceAccessResponseDto {
        TODO("Not yet implemented")
    }

    suspend fun confirmAccess(
        token: String,
        request: AccessConfirmationRequestDto
    ) {
        TODO("Not yet implemented")
    }

    suspend fun checkOut(token: String, request: CheckOutRequestDto) {
        TODO("Not yet implemented")
    }
}
