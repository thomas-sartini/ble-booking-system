package ch.hslu.blebooking.mobile.communication.networking

import ch.hslu.blebooking.mobile.communication.dto.request.RefreshTokenRequestDto
import ch.hslu.blebooking.mobile.communication.dto.response.RefreshTokenResponseDto
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

class RefreshApiClientTest {
    @Test
    fun refreshSendsJsonAndReadsRotatedTokens() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("https://mock.test/refresh", request.url.toString())
            assertTrue(request.body.contentType?.match(ContentType.Application.Json) == true)
            assertEquals(buildJsonObject { put("refreshToken", "old-refresh") },
                Json.parseToJsonElement(request.body.toByteArray().decodeToString()))
            respond("""{"accessToken":"new-access","refreshToken":"new-refresh","expiresIn":900}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory.create(engine, "https://mock.test/")
        try {
            val result = assertIs<ApiResult.Success<RefreshTokenResponseDto>>(
                BackendApiClient(client).refresh(RefreshTokenRequestDto("old-refresh")))
            assertEquals("new-access", result.data.accessToken)
            assertEquals("new-refresh", result.data.refreshToken)
        } finally { client.close() }
    }

    @Test
    fun refreshRejectsMalformedOrIncompleteResponses() = runTest {
        for (body in listOf("{ invalid", """{"accessToken":"new-access"}""")) {
            val client = HttpClientFactory.create(MockEngine {
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }, "https://mock.test/")
            try {
                val result = assertIs<ApiResult.DecodingError>(
                    BackendApiClient(client).refresh(RefreshTokenRequestDto("old")))
                assertNotNull(result.cause)
            } finally { client.close() }
        }
    }

    @Test
    fun refreshPreservesHttpErrors() = runTest {
        for (status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.InternalServerError)) {
            val client = HttpClientFactory.create(MockEngine {
                respond("refresh failed", status)
            }, "https://mock.test/")
            try {
                val result = assertIs<ApiResult.HttpError>(
                    BackendApiClient(client).refresh(RefreshTokenRequestDto("old")))
                assertEquals(status, result.status)
                assertEquals("refresh failed", result.message)
            } finally { client.close() }
        }
    }

    @Test
    fun refreshMapsNetworkFailure() = runTest {
        val client = HttpClientFactory.create(MockEngine { throw UnresolvedAddressException() },
            "https://mock.test/")
        try {
            val result = assertIs<ApiResult.NetworkError>(
                BackendApiClient(client).refresh(RefreshTokenRequestDto("old")))
            assertIs<UnresolvedAddressException>(result.cause)
        } finally { client.close() }
    }
}
