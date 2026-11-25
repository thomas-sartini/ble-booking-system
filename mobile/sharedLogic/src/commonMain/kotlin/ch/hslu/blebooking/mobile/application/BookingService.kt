package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.communication.dto.model.BookingDto
import ch.hslu.blebooking.mobile.communication.networking.ApiClient
import ch.hslu.blebooking.mobile.communication.networking.ApiResult
import ch.hslu.blebooking.mobile.model.Booking
import ch.hslu.blebooking.mobile.model.BookingStatus
import ch.hslu.blebooking.mobile.model.error.MobileCoreErrorCode
import ch.hslu.blebooking.mobile.model.error.MobileCoreException
import kotlin.time.Instant

internal class BookingService(private val apiClient: ApiClient) {
    suspend fun getActiveBookings(): List<Booking> = when (val result = apiClient.getActiveBookings()) {
        is ApiResult.Success -> result.data.map { it.toBooking() }.filter {
            // The backend applies ownership and end-time filtering using its own clock.
            it.status == BookingStatus.BOOKED || it.status == BookingStatus.CHECKED_IN
        }
        is ApiResult.Error -> throw result.toMobileCoreException()
    }

    private fun BookingDto.toBooking(): Booking = try {
        val startTime = Instant.parse(start)
        val endTime = Instant.parse(end)
        require(endTime > startTime) { "Invalid booking interval" }
        Booking(
            id = id.toString(),
            resourceId = resource.id.toString(),
            startTime = startTime,
            endTime = endTime,
            status = BookingStatus.valueOf(status),
            resourceName = resource.name,
            resourcePosition = resource.position,
        )
    } catch (e: IllegalArgumentException) {
        throw MobileCoreException(MobileCoreErrorCode.UNKNOWN_ERROR, "Invalid booking response", e)
    }

    private fun ApiResult.Error.toMobileCoreException(): MobileCoreException {
        val code = when (this) {
            is ApiResult.HttpError -> if (status.value in 500..599) {
                MobileCoreErrorCode.SERVER_ERROR
            } else {
                MobileCoreErrorCode.fromHttpStatus(status.value)
            }
            is ApiResult.NetworkError, is ApiResult.Timeout -> MobileCoreErrorCode.NETWORK_ERROR
            is ApiResult.DecodingError, is ApiResult.UnexpectedError -> MobileCoreErrorCode.UNKNOWN_ERROR
        }
        return MobileCoreException(code, message.ifBlank { code.message }, cause)
    }
}
