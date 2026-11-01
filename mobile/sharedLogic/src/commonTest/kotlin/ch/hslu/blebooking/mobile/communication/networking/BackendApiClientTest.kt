
package ch.hslu.blebooking.mobile.communication.networking

import ch.hslu.blebooking.mobile.communication.dto.request.LoginRequestDto
import ch.hslu.blebooking.mobile.communication.dto.response.LoginResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.JsonConvertException
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BackendApiClientTest {

    private lateinit var httpClient: HttpClient
    private lateinit var apiClient: BackendApiClient

    private var mockStatus = HttpStatusCode.OK
    private var mockBody = ""
    private var mockFailure: Throwable? = null

    private var receivedMethod: HttpMethod? = null
    private var receivedUrl: String? = null
    private var receivedContentType: ContentType? = null
    private var receivedBody: String? = null

    private fun loginRequest() = LoginRequestDto(
        identifier = "test@user.com",
        password = "testpassword"
    )

    @BeforeTest
    fun setUp() {
        mockStatus = HttpStatusCode.OK
        mockBody = """
            {
                "accessToken": "access-token",
                "refreshToken": "refresh-token"
            }
        """.trimIndent()

        mockFailure = null
        receivedMethod = null
        receivedUrl = null
        receivedContentType = null
        receivedBody = null

        val engine = MockEngine { request ->
            mockFailure?.let { throw it }

            receivedMethod = request.method
            receivedUrl = request.url.toString()
            receivedContentType = request.body.contentType
            receivedBody = request.body.toByteArray().decodeToString()

            respond(
                content = mockBody,
                status = mockStatus,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    "application/json"
                )
            )
        }

        httpClient = HttpClientFactory.create(
            engine = engine,
            baseUrl = "https://mock.test/api/"
        )

        apiClient = BackendApiClient(httpClient)
    }

    @AfterTest
    fun tearDown() {
        httpClient.close()
    }

    @Test
    fun testLoginSuccess() = runTest {
        val result = apiClient.login(loginRequest())

        val success =
            assertIs<ApiResult.Success<LoginResponseDto>>(result)

        assertEquals(HttpStatusCode.OK, success.status)
        assertEquals("access-token", success.data.accessToken)
        assertEquals("refresh-token", success.data.refreshToken)
    }

    @Test
    fun testLoginUnauthorized() = runTest {
        mockStatus = HttpStatusCode.Unauthorized
        mockBody = """{"message":"Invalid credentials"}"""

        val result = apiClient.login(loginRequest())

        val error = assertIs<ApiResult.Error>(result)

        assertEquals(HttpStatusCode.Unauthorized, error.status)
        assertEquals(mockBody, error.message)
    }

    @Test
    fun testLoginServerError() = runTest {
        mockStatus = HttpStatusCode.InternalServerError
        mockBody = "Internal server error"

        val result = apiClient.login(loginRequest())

        val error = assertIs<ApiResult.Error>(result)

        assertEquals(
            HttpStatusCode.InternalServerError,
            error.status
        )
        assertEquals(mockBody, error.message)
    }


    @Test
    fun testLoginInvalidJson() = runTest {
        mockBody = """{ invalid json }"""

        val result = apiClient.login(loginRequest())

        val error = assertIs<ApiResult.Error>(result)

        assertEquals(HttpStatusCode.BadGateway, error.status)
        assertEquals("Failed to process the server response", error.message)
    }



    @Test
    fun testLoginMissingFields() = runTest {
        mockBody = """
        {
            "accessToken": "access-token"
        }
    """.trimIndent()

        val result = apiClient.login(loginRequest())

        val error = assertIs<ApiResult.Error>(result)

        assertEquals(HttpStatusCode.BadGateway, error.status)
        assertEquals("Failed to process the server response", error.message)
    }


    @Test
    fun testLoginUnknownFields() = runTest {
        mockBody = """
            {
                "accessToken": "access-token",
                "refreshToken": "refresh-token",
                "extraField": "ignored-value"
            }
        """.trimIndent()

        val result = apiClient.login(loginRequest())

        val success =
            assertIs<ApiResult.Success<LoginResponseDto>>(result)

        assertEquals(HttpStatusCode.OK, success.status)
        assertEquals("access-token", success.data.accessToken)
        assertEquals("refresh-token", success.data.refreshToken)
    }

    @Test
    fun testLoginRequestBody() = runTest {
        val request = loginRequest()

        val result = apiClient.login(request)

        assertIs<ApiResult.Success<LoginResponseDto>>(result)

        assertEquals(HttpMethod.Post, receivedMethod)

        assertEquals(
            "https://mock.test/api/login",
            receivedUrl
        )

        assertTrue(
            receivedContentType?.match(
                ContentType.Application.Json
            ) == true
        )

        val body = Json.parseToJsonElement(
            assertNotNull(receivedBody)
        ).jsonObject

        assertEquals(
            request.identifier,
            body["identifier"]?.jsonPrimitive?.content
        )

        assertEquals(
            request.password,
            body["password"]?.jsonPrimitive?.content
        )
    }

    @Test
    fun testLoginNetworkFailure() = runTest {
        mockFailure = UnresolvedAddressException()

        val result = apiClient.login(loginRequest())

        val error = assertIs<ApiResult.Error>(result)

        assertEquals(
            HttpStatusCode.ServiceUnavailable,
            error.status
        )

        assertEquals(
            "Unable to connect to the server",
            error.message
        )
    }

    @Test
    fun testLoginTimeout() = runTest {
        mockFailure = ConnectTimeoutException(
            "Connection timed out"
        )

        val result = apiClient.login(loginRequest())

        val error = assertIs<ApiResult.Error>(result)

        assertEquals(
            HttpStatusCode.GatewayTimeout,
            error.status
        )

        assertEquals(
            "Connection to the server timed out",
            error.message
        )
    }
}
