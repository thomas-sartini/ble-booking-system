
package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.communication.CommunicationGateway
import ch.hslu.blebooking.mobile.model.AccessConfirmation
import ch.hslu.blebooking.mobile.model.ResourceAccessRequest
import ch.hslu.blebooking.mobile.model.ResourceAccessResponse
import ch.hslu.blebooking.mobile.model.error.MobileCoreErrorCode
import ch.hslu.blebooking.mobile.model.error.MobileCoreException
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import dev.mokkery.verify.VerifyMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Instant

class
AccessServiceTest {

    @Test
    @Ignore // AccessService not implemented yet
    fun testRequestAccessReturnsResponse() = runTest {
        // Arrange
        val gateway = mock<CommunicationGateway>()

        val request = ResourceAccessRequest(
            resourceId = "resource-123",
            bookingId = "booking-456",
            deviceId = "device-789"
        )

        val expectedResponse = ResourceAccessResponse(
            ticket = "signed-ticket",
            expiresAt = Instant.parse(
                "2026-10-16T12:00:00Z"
            )
        )

        everySuspend {
            gateway.requestAccess(request)
        } returns expectedResponse

        val service = AccessService(gateway)

        // Act
        val result = service.requestAccess(request)

        // Assert
        assertEquals(expectedResponse, result)

        // Verify that the gateway method was called
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.requestAccess(request)
        }
    }

    @Test
    @Ignore // AccessService not implemented yet
    fun testRequestAccessThrowsOnGatewayError() = runTest {
        // Arrange
        val gateway = mock<CommunicationGateway>()

        val request = ResourceAccessRequest(
            resourceId = "resource-123",
            bookingId = "booking-456",
            deviceId = "device-789"
        )

        val expectedException = MobileCoreException(
            code = MobileCoreErrorCode.FORBIDDEN,
            message = "Access denied"
        )

        everySuspend {
            gateway.requestAccess(request)
        } throws expectedException

        val service = AccessService(gateway)

        // Act & Assert
        val actualException =
            assertFailsWith<MobileCoreException> {
                service.requestAccess(request)
            }
        assertSame(expectedException, actualException)

        // Verify that the gateway method was called
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.requestAccess(request)
        }
    }

    @Test
    @Ignore // AccessService not implemented yet
    fun testConfirmAccessSuccess() = runTest {
        // Arrange
        val gateway = mock<CommunicationGateway>()

        val confirmation = AccessConfirmation(
            bookingId = "booking-123",
            signedReceipt = "signed-receipt"
        )

        everySuspend {
            gateway.confirmAccess(confirmation)
        } returns Unit

        val service = AccessService(gateway)

        // Act
        val result = runCatching {
            service.confirmAccess(confirmation)
        }

        // Assert
        assertTrue(result.isSuccess)

        // Verify that the gateway method was called
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.confirmAccess(confirmation)
        }
    }

    @Test
    @Ignore // AccessService not implemented yet
    fun testConfirmAccessThrowsOnGatewayError() = runTest {
        // Arrange
        val gateway = mock<CommunicationGateway>()

        val confirmation = AccessConfirmation(
            bookingId = "booking-123",
            signedReceipt = "invalid-receipt"
        )

        val expectedException = MobileCoreException(
            code = MobileCoreErrorCode.INVALID_RECEIPT,
            message = "Invalid access receipt"
        )

        everySuspend {
            gateway.confirmAccess(confirmation)
        } throws expectedException

        val service = AccessService(gateway)

        // Act & Assert
        val actualException = assertFailsWith<MobileCoreException> {
            service.confirmAccess(confirmation)
        }
        assertSame(expectedException, actualException)

        // Verify that the gateway method was called
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.confirmAccess(confirmation)
        }
    }

    @Test
    @Ignore // AccessService not implemented yet
    fun testCheckOutSuccess() = runTest {
        // Arrange
        val gateway = mock<CommunicationGateway>()
        val resourceId = "resource-123"

        everySuspend {
            gateway.checkOut(resourceId)
        } returns Unit

        val service = AccessService(gateway)

        // Act
        val result = runCatching {
            service.checkOut(resourceId)
        }

        // Assert
        assertTrue(result.isSuccess)

        // Verify that the gateway method was called
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.checkOut(resourceId)
        }
    }

    @Test
    @Ignore // AccessService not implemented yet
    fun testCheckOutThrowsOnGatewayError() = runTest {
        // Arrange
        val gateway = mock<CommunicationGateway>()
        val resourceId = "resource-123"

        val expectedException = MobileCoreException(
            code = MobileCoreErrorCode.CONNECTION_ERROR,
            message = "Backend unavailable"
        )

        everySuspend {
            gateway.checkOut(resourceId)
        } throws expectedException

        val service = AccessService(gateway)

        // Act & Assert
        val actualException = assertFailsWith<MobileCoreException> {
            service.checkOut(resourceId)
        }
        assertSame(expectedException, actualException)

        // Verify that the gateway method was called
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.checkOut(resourceId)
        }
    }
}
