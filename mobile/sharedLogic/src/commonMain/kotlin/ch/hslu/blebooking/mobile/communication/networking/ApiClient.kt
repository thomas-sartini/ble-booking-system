package ch.hslu.blebooking.mobile.communication.networking

import ch.hslu.blebooking.mobile.communication.dto.model.BookingDto
import ch.hslu.blebooking.mobile.communication.dto.request.AccessConfirmationRequestDto
import ch.hslu.blebooking.mobile.communication.dto.request.CheckOutRequestDto
import ch.hslu.blebooking.mobile.communication.dto.request.LoginRequestDto
import ch.hslu.blebooking.mobile.communication.dto.request.RefreshTokenRequestDto
import ch.hslu.blebooking.mobile.communication.dto.request.ResourceAccessRequestDto
import ch.hslu.blebooking.mobile.communication.dto.response.LoginResponseDto
import ch.hslu.blebooking.mobile.communication.dto.response.RefreshTokenResponseDto
import ch.hslu.blebooking.mobile.communication.dto.response.ResourceAccessResponseDto


internal interface ApiClient {

    suspend fun login(
        request: LoginRequestDto
    ): ApiResult<LoginResponseDto>

    suspend fun refresh(
        request: RefreshTokenRequestDto
    ): RefreshTokenResponseDto

    suspend fun logout(
        refreshToken: String
    )

    suspend fun getActiveBookings(
        token: String
    ): List<BookingDto>

    suspend fun requestAccess(
        token: String,
        request: ResourceAccessRequestDto
    ): ResourceAccessResponseDto

    suspend fun confirmAccess(
        token: String,
        request: AccessConfirmationRequestDto
    )

    suspend fun checkOut(
        token: String,
        request: CheckOutRequestDto
    )
}

