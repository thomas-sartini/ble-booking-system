package ch.hslu.blebooking.mobile.communication.networking

import ch.hslu.blebooking.mobile.communication.dto.model.BookingDto
import ch.hslu.blebooking.mobile.communication.dto.request.*
import ch.hslu.blebooking.mobile.communication.dto.response.*

internal interface ApiClient {

    suspend fun login(
        request: LoginRequestDto
    ): ApiResult<LoginResponseDto>

    suspend fun refresh(
        request: RefreshTokenRequestDto
    ): ApiResult<RefreshTokenResponseDto>

    suspend fun logout(
        request: LogoutRequestDto
    ): ApiResult<Unit>

    suspend fun getActiveBookings(
        token: String
    ): ApiResult<List<BookingDto>>

    suspend fun requestAccess(
        token: String,
        request: ResourceAccessRequestDto
    ): ApiResult<ResourceAccessResponseDto>

    suspend fun confirmAccess(
        token: String,
        request: AccessConfirmationRequestDto
    ): ApiResult<Unit>

    suspend fun checkOut(
        token: String,
        request: CheckOutRequestDto
    ): ApiResult<Unit>
}
