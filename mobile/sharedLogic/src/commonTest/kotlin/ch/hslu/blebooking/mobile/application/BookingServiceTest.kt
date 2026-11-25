package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.communication.dto.model.*
import ch.hslu.blebooking.mobile.communication.networking.*
import ch.hslu.blebooking.mobile.model.BookingStatus
import ch.hslu.blebooking.mobile.model.error.*
import dev.mokkery.*
import dev.mokkery.answering.returns
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Instant

class BookingServiceTest {
    private val dto = BookingDto(5000000000L, BookingResourceDto(42, "Room A", "Floor 2"),
        "2026-11-25T10:00:00+01:00", "2026-11-25T11:00:00+01:00", "BOOKED")

    @Test
    fun mapsFieldsAndFiltersTerminalStates() = runTest {
        val api = mock<ApiClient>()
        everySuspend { api.getActiveBookings() } returns ApiResult.Success(HttpStatusCode.OK,
            BookingStatus.entries.map { dto.copy(status = it.name) })
        val bookings = BookingService(api).getActiveBookings()
        assertEquals(listOf(BookingStatus.BOOKED, BookingStatus.CHECKED_IN), bookings.map { it.status })
        val booking = bookings.first()
        assertEquals("5000000000", booking.id)
        assertEquals("42", booking.resourceId)
        assertEquals("Room A", booking.resourceName)
        assertEquals("Floor 2", booking.resourcePosition)
        assertEquals(Instant.parse("2026-11-25T09:00:00Z"), booking.startTime)
        assertEquals(Instant.parse("2026-11-25T10:00:00Z"), booking.endTime)
    }

    @Test
    fun emptyResultIsValid() = runTest {
        val api = mock<ApiClient>()
        everySuspend { api.getActiveBookings() } returns ApiResult.Success(HttpStatusCode.OK, emptyList())
        assertTrue(BookingService(api).getActiveBookings().isEmpty())
    }

    @Test
    fun malformedDomainValuesAreReported() = runTest {
        for (invalid in listOf(dto.copy(start = "bad"), dto.copy(status = "UNKNOWN"), dto.copy(end = dto.start))) {
            val api = mock<ApiClient>()
            everySuspend { api.getActiveBookings() } returns ApiResult.Success(HttpStatusCode.OK, listOf(invalid))
            assertEquals(MobileCoreErrorCode.UNKNOWN_ERROR,
                assertFailsWith<MobileCoreException> { BookingService(api).getActiveBookings() }.code)
        }
    }

    @Test
    fun expectedErrorsAreMapped() = runTest {
        val cause = IllegalStateException("failure")
        val cases = listOf(
            ApiResult.HttpError(HttpStatusCode.Unauthorized, "denied") to MobileCoreErrorCode.UNAUTHORIZED,
            ApiResult.HttpError(HttpStatusCode.Forbidden, "denied") to MobileCoreErrorCode.FORBIDDEN,
            ApiResult.HttpError(HttpStatusCode.ServiceUnavailable, "unavailable") to MobileCoreErrorCode.SERVER_ERROR,
            ApiResult.NetworkError(cause) to MobileCoreErrorCode.NETWORK_ERROR,
            ApiResult.Timeout(cause) to MobileCoreErrorCode.NETWORK_ERROR,
            ApiResult.DecodingError(cause) to MobileCoreErrorCode.UNKNOWN_ERROR,
        )
        for ((error, code) in cases) {
            val api = mock<ApiClient>()
            everySuspend { api.getActiveBookings() } returns error
            val failure = assertFailsWith<MobileCoreException> { BookingService(api).getActiveBookings() }
            assertEquals(code, failure.code)
            assertEquals(error.message, failure.message)
            assertSame(error.cause, failure.cause)
        }
    }
}
