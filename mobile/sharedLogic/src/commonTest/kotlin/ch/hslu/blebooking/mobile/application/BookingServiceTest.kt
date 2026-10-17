
package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.communication.CommunicationGateway
import ch.hslu.blebooking.mobile.model.Booking
import ch.hslu.blebooking.mobile.model.BookingStatus
import ch.hslu.blebooking.mobile.model.error.MobileCoreErrorCode
import ch.hslu.blebooking.mobile.model.error.MobileCoreException
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.time.Instant
import dev.mokkery.verify.VerifyMode

class BookingServiceTest {

    @Test
    @Ignore() // BookingService not implemented yet
    fun testGetActiveBookingsReturnsEmptyList() = runTest {
        // Arrange
        val gateway = mock<CommunicationGateway>()
        everySuspend {
            gateway.getActiveBookings()
        } returns emptyList()

        val service = BookingService(gateway)

        // Act
        val result = service.getActiveBookings()

        // Assert
        assertEquals(emptyList(), result)

        // Verify that the gateway method was called
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.getActiveBookings()
        }
    }

    @Test
    @Ignore() // BookingService not implemented yet
    fun testGetActiveBookingsReturnsMultipleBookings() = runTest {
        // Arrange
        val gateway = mock<CommunicationGateway>()

        val bookings = listOf(
            Booking(
                id = "1",
                resourceId = "resource1",
                startTime = Instant.parse("2023-01-01T10:00:00Z"),
                endTime = Instant.parse("2023-01-01T12:00:00Z"),
                status = BookingStatus.BOOKED
            ),
            Booking(
                id = "2",
                resourceId = "resource2",
                startTime = Instant.parse("2023-01-02T14:30:00Z"),
                endTime = Instant.parse("2023-01-02T16:30:00Z"),
                status = BookingStatus.CHECKED_IN
            ),
            Booking(
                id = "3",
                resourceId = "resource3",
                startTime = Instant.parse("2023-01-03T09:15:00Z"),
                endTime = Instant.parse("2023-01-03T11:45:00Z"),
                status = BookingStatus.CHECKED_IN
            )
        )

        everySuspend {
            gateway.getActiveBookings()
        } returns bookings


        val service = BookingService(gateway)

        // Act
        val result = service.getActiveBookings()

        // Assert
        assertEquals(bookings, result)

        // Verify that the gateway method was called
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.getActiveBookings()
        }
    }

    @Test
    @Ignore() // BookingService not implemented yet
    fun testGetActiveBookingsThrowsOnGatewayError() = runTest {
        // Arrange
        val gateway = mock<CommunicationGateway>()

        val expectedException = MobileCoreException(
            code = MobileCoreErrorCode.CONNECTION_ERROR,
            message = "Backend unavailable"
        )

        everySuspend {
            gateway.getActiveBookings()
        } throws expectedException

        val service = BookingService(gateway)

        // Act & Assert
        val actualException = assertFailsWith<MobileCoreException> {
            service.getActiveBookings()
        }
        assertSame(expectedException, actualException)

        verifySuspend(VerifyMode.exactly(1)) {
            gateway.getActiveBookings()
        }
    }
}
