
package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.communication.networking.ApiClient
import ch.hslu.blebooking.mobile.communication.networking.ApiResult
import ch.hslu.blebooking.mobile.communication.dto.request.LoginRequestDto
import ch.hslu.blebooking.mobile.communication.dto.request.LogoutRequestDto
import ch.hslu.blebooking.mobile.communication.dto.response.LoginResponseDto
import ch.hslu.blebooking.mobile.token.TokenStorage
import dev.mokkery.every
import io.ktor.http.HttpStatusCode
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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AuthServiceTest {

    @Test
    fun testLoginSuccess() = runTest {
        // Arrange
        val apiClient = mock<ApiClient>()
        val storage = mock<TokenStorage>()
        every { storage.getRefreshToken() } returns "refresh-token"
        every { storage.storeTokens("access-token", "refresh-token") } returns Unit
        every { storage.clearTokens() } returns Unit
        val identifier = "test@user.com"
        val password = "testpassword"

        everySuspend {
            apiClient.login(LoginRequestDto(identifier, password))
        } returns ApiResult.Success(HttpStatusCode.OK, LoginResponseDto("access-token", "refresh-token"))

        val service = AuthService(storage, apiClient)

        // Act
        val result = runCatching {
            service.login(identifier, password)
        }

        // Assert
        assertTrue(result.isSuccess)

        // Verify that the API client method was called
        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.login(LoginRequestDto(identifier, password))
        }
    }

    @Test
    fun testLoginWithInvalidMail() = runTest {
        // Arrange
        val apiClient = mock<ApiClient>()
        val storage = mock<TokenStorage>()
        every { storage.getRefreshToken() } returns "refresh-token"
        every { storage.storeTokens("access-token", "refresh-token") } returns Unit
        every { storage.clearTokens() } returns Unit
        val identifier = "invalid-mail"
        val password = "testpassword"

        everySuspend {
            apiClient.login(LoginRequestDto(identifier, password))
        } returns ApiResult.Success(HttpStatusCode.OK, LoginResponseDto("access-token", "refresh-token"))

        val service = AuthService(storage, apiClient)

        // Act & Assert
        val actualException = assertFailsWith<MobileCoreException> {
            service.login(identifier, password)
        }
        // Assert
        assertEquals(
            MobileCoreErrorCode.VALIDATION_ERROR,
            actualException.code
        )

        assertEquals(
            "Invalid email",
            actualException.message
        )

        // Verify
        verifySuspend(VerifyMode.exactly(0)) {
            apiClient.login(LoginRequestDto(identifier, password))
        }
    }

    @Test
    fun testLoginWithEmptyMail() = runTest {
        // Arrange
        val apiClient = mock<ApiClient>()
        val storage = mock<TokenStorage>()
        every { storage.getRefreshToken() } returns "refresh-token"
        every { storage.storeTokens("access-token", "refresh-token") } returns Unit
        every { storage.clearTokens() } returns Unit
        val identifier = ""
        val password = "testpassword"

        everySuspend {
            apiClient.login(LoginRequestDto(identifier, password))
        } returns ApiResult.Success(HttpStatusCode.OK, LoginResponseDto("access-token", "refresh-token"))

        val service = AuthService(storage, apiClient)

        // Act & Assert
        val actualException = assertFailsWith<MobileCoreException> {
            service.login(identifier, password)
        }
        // Assert
        assertEquals(
            MobileCoreErrorCode.VALIDATION_ERROR,
            actualException.code
        )

        assertEquals(
            "Invalid email",
            actualException.message
        )

        // Verify
        verifySuspend(VerifyMode.exactly(0)) {
            apiClient.login(LoginRequestDto(identifier, password))
        }
    }

    @Test
    fun testLoginThrowsOnApiClientError() = runTest {
        // Arrange
        val apiClient = mock<ApiClient>()
        val storage = mock<TokenStorage>()
        every { storage.getRefreshToken() } returns "refresh-token"
        every { storage.storeTokens("access-token", "refresh-token") } returns Unit
        every { storage.clearTokens() } returns Unit
        val identifier = "test@user.com"
        val password = "wrongpassword"

        val expectedException = MobileCoreException(
            code = MobileCoreErrorCode.UNAUTHORIZED,
            message = "Invalid credentials"
        )

        everySuspend {
            apiClient.login(LoginRequestDto(identifier, password))
        } throws expectedException

        val service = AuthService(storage, apiClient)

        // Act & Assert
        val actualException = assertFailsWith<MobileCoreException> {
            service.login(identifier, password)
        }
        assertSame(expectedException, actualException)

        // Verify that the API client method was called
        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.login(LoginRequestDto(identifier, password))
        }
    }

    @Test
    fun testLogoutSuccess() = runTest {
        // Arrange
        val apiClient = mock<ApiClient>()
        val storage = mock<TokenStorage>()
        every { storage.getRefreshToken() } returns "refresh-token"
        every { storage.storeTokens("access-token", "refresh-token") } returns Unit
        every { storage.clearTokens() } returns Unit

        everySuspend {
            apiClient.logout(LogoutRequestDto("refresh-token"))
        } returns ApiResult.Success(HttpStatusCode.OK, Unit)

        val service = AuthService(storage, apiClient)

        // Act
        val result = runCatching {
            service.logout()
        }

        // Assert
        assertTrue(result.isSuccess)

        // Verify that the API client method was called
        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.logout(LogoutRequestDto("refresh-token"))
        }
    }

    @Test
    fun testLogoutThrowsOnApiClientError() = runTest {
        // Arrange
        val apiClient = mock<ApiClient>()
        val storage = mock<TokenStorage>()
        every { storage.getRefreshToken() } returns "refresh-token"
        every { storage.storeTokens("access-token", "refresh-token") } returns Unit
        every { storage.clearTokens() } returns Unit

        val expectedException = MobileCoreException(
            code = MobileCoreErrorCode.NETWORK_ERROR,
            message = "Backend unavailable"
        )

        everySuspend {
            apiClient.logout(LogoutRequestDto("refresh-token"))
        } throws expectedException

        val service = AuthService(storage, apiClient)

        // Act & Assert
        val actualException = assertFailsWith<MobileCoreException> {
            service.logout()
        }
        assertSame(expectedException, actualException)

        // Verify that the API client method was called
        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.logout(LogoutRequestDto("refresh-token"))
        }
    }


}
