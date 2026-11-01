package ch.hslu.blebooking.mobile.communication

import ch.hslu.blebooking.mobile.communication.dto.request.LoginRequestDto
import ch.hslu.blebooking.mobile.communication.dto.request.LogoutRequestDto
import ch.hslu.blebooking.mobile.communication.dto.response.LoginResponseDto
import ch.hslu.blebooking.mobile.communication.networking.ApiClient
import ch.hslu.blebooking.mobile.communication.networking.ApiResult
import ch.hslu.blebooking.mobile.token.TokenStorage
import ch.hslu.blebooking.mobile.model.error.MobileCoreErrorCode
import ch.hslu.blebooking.mobile.model.error.MobileCoreException
import dev.mokkery.verify
import dev.mokkery.matcher.any
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertSame

class BackendCommunicationTest {

    @Test
    fun testLoginSuccessStoresTokens() = runTest {
        // Arrange
        val apiClient = mock<ApiClient>()
        val tokenManager = mock<TokenStorage>()

        val identifier = "test@user.com"
        val password = "testpassword"

        val request = LoginRequestDto(
            identifier = identifier,
            password = password
        )

        val response = LoginResponseDto(
            accessToken = "access-token",
            refreshToken = "refresh-token"
        )

        everySuspend {
            apiClient.login(request)
        } returns ApiResult.Success(
            status = HttpStatusCode.OK,
            data = response
        )

        everySuspend {
            tokenManager.storeTokens(
                response.accessToken,
                response.refreshToken
            )
        } returns Unit

        val communication = BackendCommunication(
            tokenStorage = tokenManager,
            apiClient = apiClient
        )

        // Act
        communication.login(identifier, password)

        // Assert
        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.login(request)
        }

        verifySuspend(VerifyMode.exactly(1)) {
            tokenManager.storeTokens(
                "access-token",
                "refresh-token"
            )
        }
    }


    @Test
    fun testLoginUnauthorized() = runTest {
        // Arrange
        val apiClient = mock<ApiClient>()
        val tokenManager = mock<TokenStorage>()

        val identifier = "test@user.com"
        val password = "wrongpassword"
        val request = LoginRequestDto(identifier, password)

        everySuspend {
            apiClient.login(request)
        } returns ApiResult.Error(
            status = HttpStatusCode.Unauthorized,
            message = "Invalid credentials"
        )

        val communication = BackendCommunication(
            tokenStorage = tokenManager,
            apiClient = apiClient
        )

        // Act
        val exception = assertFailsWith<MobileCoreException> {
            communication.login(identifier, password)
        }

        // Assert
        assertEquals(
            MobileCoreErrorCode.UNAUTHORIZED,
            exception.code
        )
        assertEquals("Invalid credentials", exception.message)

        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.login(request)
        }

        verify(VerifyMode.exactly(0)) {
            tokenManager.storeTokens(any(), any())
        }
    }

    @Test
    fun testLoginServerError() = runTest {
        // Arrange
        val apiClient = mock<ApiClient>()
        val tokenManager = mock<TokenStorage>()

        val identifier = "test@user.com"
        val password = "testpassword"
        val request = LoginRequestDto(identifier, password)

        everySuspend {
            apiClient.login(request)
        } returns ApiResult.Error(
            status = HttpStatusCode.InternalServerError,
            message = "Internal server error"
        )

        val communication = BackendCommunication(
            tokenStorage = tokenManager,
            apiClient = apiClient
        )

        // Act
        val exception = assertFailsWith<MobileCoreException> {
            communication.login(identifier, password)
        }

        // Assert
        assertEquals(
            MobileCoreErrorCode.SERVER_ERROR,
            exception.code
        )

        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.login(request)
        }

        verify(VerifyMode.exactly(0)) {
            tokenManager.storeTokens(any(), any())
        }
    }

    @Test
    fun testLoginNetworkError() = runTest {
        // Arrange
        val apiClient = mock<ApiClient>()
        val tokenManager = mock<TokenStorage>()

        val identifier = "test@user.com"
        val password = "testpassword"
        val request = LoginRequestDto(identifier, password)


        everySuspend {
            apiClient.login(request)
        } returns ApiResult.Error(
            status = HttpStatusCode.ServiceUnavailable,
            message = "Unable to connect to the server"
        )

        val communication = BackendCommunication(
            tokenStorage = tokenManager,
            apiClient = apiClient
        )

        // Act
        val exception = assertFailsWith<MobileCoreException> {
            communication.login(identifier, password)
        }

        // Assert
        assertEquals(
            MobileCoreErrorCode.NETWORK_ERROR,
            exception.code
        )

        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.login(request)
        }

        verify(VerifyMode.exactly(0)) {
            tokenManager.storeTokens(any(), any())
        }
    }


    @Test
    fun testLoginUnknownHttpError() = runTest {
        // Arrange
        val apiClient = mock<ApiClient>()
        val tokenManager = mock<TokenStorage>()

        val identifier = "test@user.com"
        val password = "testpassword"
        val request = LoginRequestDto(identifier, password)

        everySuspend {
            apiClient.login(request)
        } returns ApiResult.Error(
            status = HttpStatusCode(418, "I'm a teapot"),
            message = "Unexpected response"
        )

        val communication = BackendCommunication(
            tokenStorage = tokenManager,
            apiClient = apiClient
        )

        // Act
        val exception = assertFailsWith<MobileCoreException> {
            communication.login(identifier, password)
        }

        // Assert
        assertEquals(
            MobileCoreErrorCode.UNKNOWN_ERROR,
            exception.code
        )

        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.login(request)
        }

        verify(VerifyMode.exactly(0)) {
            tokenManager.storeTokens(any(), any())
        }
    }


    @Test
    fun testLoginTokenStorageFailure() = runTest {
        // Arrange
        val apiClient = mock<ApiClient>()
        val tokenManager = mock<TokenStorage>()

        val identifier = "test@user.com"
        val password = "testpassword"
        val request = LoginRequestDto(identifier, password)

        val response = LoginResponseDto(
            accessToken = "access-token",
            refreshToken = "refresh-token"
        )

        val expectedException = IllegalStateException(
            "Failed to store tokens"
        )

        everySuspend {
            apiClient.login(request)
        } returns ApiResult.Success(
            status = HttpStatusCode.OK,
            data = response
        )

        every {
            tokenManager.storeTokens(
                response.accessToken,
                response.refreshToken
            )
        } throws expectedException

        val communication = BackendCommunication(
            tokenStorage = tokenManager,
            apiClient = apiClient
        )

        // Act
        val exception = assertFailsWith<IllegalStateException> {
            communication.login(identifier, password)
        }

        // Assert
        assertSame(expectedException, exception)

        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.login(request)
        }

        verify(VerifyMode.exactly(1)) {
            tokenManager.storeTokens(
                "access-token",
                "refresh-token"
            )
        }
    }


    @Test
    fun testLogoutSuccessClearsTokens() = runTest {
        val apiClient = mock<ApiClient>()
        val tokenStorage = mock<TokenStorage>()

        val request = LogoutRequestDto("refresh-token")

        every {
            tokenStorage.getRefreshToken()
        } returns "refresh-token"

        everySuspend {
            apiClient.logout(request)
        } returns ApiResult.Success(
            status = HttpStatusCode.OK,
            data = Unit
        )

        every {
            tokenStorage.clearTokens()
        } returns Unit

        val communication = BackendCommunication(
            tokenStorage = tokenStorage,
            apiClient = apiClient
        )

        communication.logout()

        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.logout(request)
        }

        verify(VerifyMode.exactly(1)) {
            tokenStorage.clearTokens()
        }
    }

    @Test
    fun testLogoutWithoutRefreshToken() = runTest {
        val apiClient = mock<ApiClient>()
        val tokenStorage = mock<TokenStorage>()

        every {
            tokenStorage.getRefreshToken()
        } returns null

        every {
            tokenStorage.clearTokens()
        } returns Unit

        val communication = BackendCommunication(
            tokenStorage = tokenStorage,
            apiClient = apiClient
        )

        communication.logout()

        verifySuspend(VerifyMode.exactly(0)) {
            apiClient.logout(any())
        }

        verify(VerifyMode.exactly(1)) {
            tokenStorage.clearTokens()
        }
    }

    @Test
    fun testLogoutWithBlankRefreshToken() = runTest {
        val apiClient = mock<ApiClient>()
        val tokenStorage = mock<TokenStorage>()

        every {
            tokenStorage.getRefreshToken()
        } returns "   "

        every {
            tokenStorage.clearTokens()
        } returns Unit

        val communication = BackendCommunication(
            tokenStorage = tokenStorage,
            apiClient = apiClient
        )

        communication.logout()

        verifySuspend(VerifyMode.exactly(0)) {
            apiClient.logout(any())
        }

        verify(VerifyMode.exactly(1)) {
            tokenStorage.clearTokens()
        }
    }

    @Test
    fun testLogoutServerErrorDoesNotClearTokens() = runTest {
        val apiClient = mock<ApiClient>()
        val tokenStorage = mock<TokenStorage>()

        val request = LogoutRequestDto("refresh-token")

        every {
            tokenStorage.getRefreshToken()
        } returns "refresh-token"

        everySuspend {
            apiClient.logout(request)
        } returns ApiResult.Error(
            status = HttpStatusCode.InternalServerError,
            message = "Internal server error"
        )

        val communication = BackendCommunication(
            tokenStorage = tokenStorage,
            apiClient = apiClient
        )

        val exception = assertFailsWith<MobileCoreException> {
            communication.logout()
        }

        assertEquals(
            MobileCoreErrorCode.SERVER_ERROR,
            exception.code
        )

        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.logout(request)
        }

        verify(VerifyMode.exactly(0)) {
            tokenStorage.clearTokens()
        }
    }

    @Test
    fun testLogoutNetworkErrorDoesNotClearTokens() = runTest {
        val apiClient = mock<ApiClient>()
        val tokenStorage = mock<TokenStorage>()

        val request = LogoutRequestDto("refresh-token")

        every {
            tokenStorage.getRefreshToken()
        } returns "refresh-token"

        everySuspend {
            apiClient.logout(request)
        } returns ApiResult.Error(
            status = HttpStatusCode.ServiceUnavailable,
            message = "Unable to connect to the server"
        )

        val communication = BackendCommunication(
            tokenStorage = tokenStorage,
            apiClient = apiClient
        )

        val exception = assertFailsWith<MobileCoreException> {
            communication.logout()
        }

        assertEquals(
            MobileCoreErrorCode.NETWORK_ERROR,
            exception.code
        )

        verifySuspend(VerifyMode.exactly(1)) {
            apiClient.logout(request)
        }

        verify(VerifyMode.exactly(0)) {
            tokenStorage.clearTokens()
        }
    }


}