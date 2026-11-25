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

    fun clearAuthCache()
    suspend fun getActiveBookings(): ApiResult<List<BookingDto>>

    suspend fun requestAccess(
        request: ResourceAccessRequestDto
    ): ApiResult<ResourceAccessResponseDto>

    suspend fun confirmAccess(
        request: AccessConfirmationRequestDto
    ): ApiResult<Unit>

    suspend fun checkOut(
        request: CheckOutRequestDto
    ): ApiResult<Unit>

    suspend fun getUserProfile(): ApiResult<UserProfileResponseDto>
}
