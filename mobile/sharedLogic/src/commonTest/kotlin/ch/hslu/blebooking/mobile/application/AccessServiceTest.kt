
package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.communication.networking.ApiClient
import ch.hslu.blebooking.mobile.communication.networking.ApiResult
import ch.hslu.blebooking.mobile.communication.dto.model.BookingDto
import ch.hslu.blebooking.mobile.communication.dto.response.ResourceAccessResponseDto
import dev.mokkery.matcher.any
import io.ktor.http.HttpStatusCode
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
        // Arrange (DTO fixtures remain provisional until this operation is implemented.)
        val apiClient = mock<ApiClient>()

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
            apiClient.requestAccess(any(), any())
        } returns ApiResult.Success(HttpStatusCode.OK, ResourceAccessResponseDto(1))

        val service = AccessService(apiClient)

        // Act
        val result = service.requestAccess(request)

        // Assert
        assertEquals(expectedResponse, result)

        // Verify that the apiClient method was called
        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.requestAccess(any(), any())
        }
    }

    @Test
    @Ignore // AccessService not implemented yet
    fun testRequestAccessThrowsOnApiClientError() = runTest {
        // Arrange (DTO fixtures remain provisional until this operation is implemented.)
        val apiClient = mock<ApiClient>()

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
            apiClient.requestAccess(any(), any())
        } throws expectedException

        val service = AccessService(apiClient)

        // Act & Assert
        val actualException =
            assertFailsWith<MobileCoreException> {
                service.requestAccess(request)
            }
        assertSame(expectedException, actualException)

        // Verify that the apiClient method was called
        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.requestAccess(any(), any())
        }
    }

    @Test
    @Ignore // AccessService not implemented yet
    fun testConfirmAccessSuccess() = runTest {
        // Arrange (DTO fixtures remain provisional until this operation is implemented.)
        val apiClient = mock<ApiClient>()

        val confirmation = AccessConfirmation(
            bookingId = "booking-123",
            signedReceipt = "signed-receipt"
        )

        everySuspend {
            apiClient.confirmAccess(any(), any())
        } returns ApiResult.Success(HttpStatusCode.OK, Unit)

        val service = AccessService(apiClient)

        // Act
        val result = runCatching {
            service.confirmAccess(confirmation)
        }

        // Assert
        assertTrue(result.isSuccess)

        // Verify that the apiClient method was called
        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.confirmAccess(any(), any())
        }
    }

    @Test
    @Ignore // AccessService not implemented yet
    fun testConfirmAccessThrowsOnApiClientError() = runTest {
        // Arrange (DTO fixtures remain provisional until this operation is implemented.)
        val apiClient = mock<ApiClient>()

        val confirmation = AccessConfirmation(
            bookingId = "booking-123",
            signedReceipt = "invalid-receipt"
        )

        val expectedException = MobileCoreException(
            code = MobileCoreErrorCode.VALIDATION_ERROR,
            message = "Invalid access receipt"
        )

        everySuspend {
            apiClient.confirmAccess(any(), any())
        } throws expectedException

        val service = AccessService(apiClient)

        // Act & Assert
        val actualException = assertFailsWith<MobileCoreException> {
            service.confirmAccess(confirmation)
        }
        assertSame(expectedException, actualException)

        // Verify that the apiClient method was called
        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.confirmAccess(any(), any())
        }
    }

    @Test
    @Ignore // AccessService not implemented yet
    fun testCheckOutSuccess() = runTest {
        // Arrange (DTO fixtures remain provisional until this operation is implemented.)
        val apiClient = mock<ApiClient>()
        val resourceId = "resource-123"

        everySuspend {
            apiClient.checkOut(any(), any())
        } returns ApiResult.Success(HttpStatusCode.OK, Unit)

        val service = AccessService(apiClient)

        // Act
        val result = runCatching {
            service.checkOut(resourceId)
        }

        // Assert
        assertTrue(result.isSuccess)

        // Verify that the apiClient method was called
        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.checkOut(any(), any())
        }
    }

    @Test
    @Ignore // AccessService not implemented yet
    fun testCheckOutThrowsOnApiClientError() = runTest {
        // Arrange (DTO fixtures remain provisional until this operation is implemented.)
        val apiClient = mock<ApiClient>()
        val resourceId = "resource-123"

        val expectedException = MobileCoreException(
            code = MobileCoreErrorCode.NETWORK_ERROR,
            message = "Backend unavailable"
        )

        everySuspend {
            apiClient.checkOut(any(), any())
        } throws expectedException

        val service = AccessService(apiClient)

        // Act & Assert
        val actualException = assertFailsWith<MobileCoreException> {
            service.checkOut(resourceId)
        }
        assertSame(expectedException, actualException)

        // Verify that the apiClient method was called
        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.checkOut(any(), any())
        }
    }
}
