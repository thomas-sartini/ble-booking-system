
package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.communication.CommunicationGateway
import ch.hslu.blebooking.mobile.model.error.MobileCoreErrorCode
import ch.hslu.blebooking.mobile.model.error.MobileCoreException
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AuthServiceTest {

    @Test
    @Ignore // AuthService not implemented yet
    fun testLoginSuccess() = runTest {
        // Arrange
        val gateway = mock<CommunicationGateway>()
        val identifier = "testuser"
        val password = "testpassword"

        everySuspend {
            gateway.login(identifier, password)
        } returns Unit

        val service = AuthService(gateway)

        // Act
        val result = runCatching {
            service.login("user", "password")
        }

        // Assert
        assertTrue(result.isSuccess)

        // Verify that the gateway method was called
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.login(identifier, password)
        }
    }

    @Test
    @Ignore // AuthService not implemented yet
    fun testLoginThrowsOnGatewayError() = runTest {
        // Arrange
        val gateway = mock<CommunicationGateway>()
        val identifier = "testuser"
        val password = "wrongpassword"

        val expectedException = MobileCoreException(
            code = MobileCoreErrorCode.UNAUTHORIZED,
            message = "Invalid credentials"
        )

        everySuspend {
            gateway.login(identifier, password)
        } throws expectedException

        val service = AuthService(gateway)

        // Act & Assert
        val actualException = assertFailsWith<MobileCoreException> {
            service.login(identifier, password)
        }
        assertSame(expectedException, actualException)

        // Verify that the gateway method was called
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.login(identifier, password)
        }
    }

    @Test
    @Ignore // AuthService not implemented yet
    fun testLogoutSuccess() = runTest {
        // Arrange
        val gateway = mock<CommunicationGateway>()

        everySuspend {
            gateway.logout()
        } returns Unit

        val service = AuthService(gateway)

        // Act
        val result = runCatching {
            service.logout()
        }

        // Assert
        assertTrue(result.isSuccess)

        // Verify that the gateway method was called
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.logout()
        }
    }

    @Test
    @Ignore // AuthService not implemented yet
    fun testLogoutThrowsOnGatewayError() = runTest {
        // Arrange
        val gateway = mock<CommunicationGateway>()

        val expectedException = MobileCoreException(
            code = MobileCoreErrorCode.CONNECTION_ERROR,
            message = "Backend unavailable"
        )

        everySuspend {
            gateway.logout()
        } throws expectedException

        val service = AuthService(gateway)

        // Act & Assert
        val actualException = assertFailsWith<MobileCoreException> {
            service.logout()
        }
        assertSame(expectedException, actualException)

        // Verify that the gateway method was called
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.logout()
        }
    }


}
