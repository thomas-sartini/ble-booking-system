package ch.hslu.blebooking.mobile.communication.networking

import ch.hslu.blebooking.mobile.application.ServiceFacade
import ch.hslu.blebooking.mobile.token.TokenStorage
import ch.hslu.blebooking.mobile.model.error.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class BookingRetrievalTest {
    private class Storage : TokenStorage {
        override fun getAccessToken() = "access"
        override fun getRefreshToken() = "refresh"
        override fun storeTokens(accessToken: String, refreshToken: String) = Unit
        override fun clearTokens() = Unit
    }

    @Test
    fun retrievesOwnUpcomingBookingsThroughAuthenticatedCore() = runTest {
        val storage = Storage()
        val client = HttpClientFactory.create(MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/bookings", request.url.encodedPath)
            assertEquals(setOf("scope"), request.url.parameters.names())
            assertEquals("upcoming", request.url.parameters["scope"])
            assertEquals("Bearer access", request.headers[HttpHeaders.Authorization])
            respond("""[{"id":1,"resource":{"id":2,"name":"Lab","type":"ROOM","position":"A"},"start":"2026-11-25T10:00:00Z","end":"2026-11-25T11:00:00Z","status":"CHECKED_IN","price":"0.00","currency":"CHF","cancellable":false}]""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }, "https://mock.test/", storage)
        try {
            val bookings = ServiceFacade(storage, BackendApiClient(client)).getActiveBookings()
            assertEquals(1, bookings.size)
            assertEquals("Lab", bookings.single().resourceName)
        } finally { client.close() }
    }

    @Test
    fun unauthenticatedRequestReportsUnauthorized() = runTest {
        val client = HttpClientFactory.create(MockEngine { request ->
            assertNull(request.headers[HttpHeaders.Authorization])
            respond("Unauthorized", HttpStatusCode.Unauthorized)
        }, "https://mock.test/")
        try {
            val error = assertIs<ApiResult.HttpError>(BackendApiClient(client).getActiveBookings())
            assertEquals(HttpStatusCode.Unauthorized, error.status)
        } finally { client.close() }
    }

    @Test
    fun emptyAndMalformedResponsesAreHandled() = runTest {
        for (body in listOf("[]", "invalid json", "[{\"id\":1}]")) {
            val client = HttpClientFactory.create(MockEngine {
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }, "https://mock.test/")
            try {
                val result = BackendApiClient(client).getActiveBookings()
                if (body == "[]") assertTrue(assertIs<ApiResult.Success<*>>(result).data == emptyList<Any>())
                else assertIs<ApiResult.DecodingError>(result)
            } finally { client.close() }
        }
    }
}
