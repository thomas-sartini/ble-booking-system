package ch.hslu.blebooking.mobile.communication

import ch.hslu.blebooking.mobile.communication.dto.request.RefreshTokenRequestDto
import ch.hslu.blebooking.mobile.communication.dto.response.RefreshTokenResponseDto
import ch.hslu.blebooking.mobile.communication.networking.*
import ch.hslu.blebooking.mobile.token.TokenStorage
import ch.hslu.blebooking.mobile.model.error.*
import dev.mokkery.*
import dev.mokkery.answering.returns
import dev.mokkery.matcher.any
import dev.mokkery.verify.VerifyMode
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class RefreshCommunicationTest {
    @Test
    fun refreshStoresRotatedTokens() = runTest {
        val api = mock<ApiClient>()
        val storage = mock<TokenStorage>()
        every { storage.getRefreshToken() } returns "old"
        every { storage.clearTokens() } returns Unit
        every { storage.storeTokens("new-access", "new-refresh") } returns Unit
        everySuspend { api.refresh(RefreshTokenRequestDto("old")) } returns
            ApiResult.Success(HttpStatusCode.OK, RefreshTokenResponseDto("new-access", "new-refresh"))

        BackendCommunication(storage, api).refreshAccessToken()

        verify { storage.storeTokens("new-access", "new-refresh") }
        verifySuspend(VerifyMode.exactly(1)) { api.refresh(RefreshTokenRequestDto("old")) }
    }

    @Test
    fun missingOrBlankTokenClearsStorageWithoutRequest() = runTest {
        for (token in listOf(null, "", "   ")) {
            val api = mock<ApiClient>()
            val storage = mock<TokenStorage>()
            every { storage.getRefreshToken() } returns token
            every { storage.clearTokens() } returns Unit

            BackendCommunication(storage, api).refreshAccessToken()

            verify(VerifyMode.exactly(1)) { storage.clearTokens() }
            verifySuspend(VerifyMode.exactly(0)) { api.refresh(any()) }
            verify(VerifyMode.exactly(0)) { storage.storeTokens(any(), any()) }
        }
    }

    @Test
    fun refreshErrorsPreserveTokensAndExposeErrorCode() = runTest {
        val cases = listOf(
            HttpStatusCode.Unauthorized to MobileCoreErrorCode.UNAUTHORIZED,
            HttpStatusCode.InternalServerError to MobileCoreErrorCode.SERVER_ERROR,
            HttpStatusCode.ServiceUnavailable to MobileCoreErrorCode.NETWORK_ERROR
        )
        for ((status, code) in cases) {
            val api = mock<ApiClient>()
            val storage = mock<TokenStorage>()
            every { storage.getRefreshToken() } returns "old"
            everySuspend { api.refresh(RefreshTokenRequestDto("old")) } returns
                ApiResult.Error(status, "refresh failed")

            val error = assertFailsWith<MobileCoreException> {
                BackendCommunication(storage, api).refreshAccessToken()
            }

            assertEquals(code, error.code)
            assertEquals("refresh failed", error.message)
            verify(VerifyMode.exactly(0)) { storage.clearTokens() }
            verify(VerifyMode.exactly(0)) { storage.storeTokens(any(), any()) }
        }
    }
}
