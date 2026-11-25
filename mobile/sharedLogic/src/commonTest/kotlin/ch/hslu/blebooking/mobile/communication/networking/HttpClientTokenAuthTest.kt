package ch.hslu.blebooking.mobile.communication.networking

import ch.hslu.blebooking.mobile.token.TokenStorage
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*

class HttpClientTokenAuthTest {

    @Test
    fun restoreSessionWithoutCredentialsDoesNotContactBackend() = runTest {
        val storage = FakeTokenStorage()
        val client = HttpClientFactory.create(MockEngine { error("Unexpected request") }, "https://mock.test/", storage)
        try {
            val core = ch.hslu.blebooking.mobile.application.ServiceFacade(storage, BackendApiClient(client))
            assertEquals(ch.hslu.blebooking.mobile.model.SessionRestoreResult.Unauthenticated, core.restoreSession())
        } finally {
            client.close()
        }
    }

    @Test
    fun restoreSessionPreservesCredentialsOnTransientRefreshFailureAndCanRetry() = runTest {
        for (failure in listOf("offline", "server", "cancelled")) {
            val storage = FakeTokenStorage(refreshToken = "saved-refresh")
            var fail = true
            val engine = MockEngine { request ->
                if (request.url.encodedPath == "/auth/refresh") {
                    if (fail) {
                        when (failure) {
                            "offline" -> throw io.ktor.util.network.UnresolvedAddressException()
                            "cancelled" -> throw kotlinx.coroutines.CancellationException("Cancelled")
                            else -> respond("Unavailable", HttpStatusCode.ServiceUnavailable)
                        }
                    } else {
                        respond("""{"accessToken":"new-access","refreshToken":"new-refresh"}""",
                            HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                    }
                } else if (request.headers[HttpHeaders.Authorization] == "Bearer new-access") {
                    respond("""{"firstName":"Alice","lastName":"Example","email":"alice@example.com","username":"alice","role":"STUDENT"}""",
                        HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                } else {
                    respond("Unauthorized", HttpStatusCode.Unauthorized)
                }
            }
            val client = HttpClientFactory.create(engine, "https://mock.test/", storage)
            try {
                val core = ch.hslu.blebooking.mobile.application.ServiceFacade(storage, BackendApiClient(client))
                if (failure == "cancelled") {
                    assertFailsWith<kotlinx.coroutines.CancellationException> { core.restoreSession() }
                } else {
                    assertIs<ch.hslu.blebooking.mobile.model.SessionRestoreResult.Failed>(core.restoreSession())
                }
                assertEquals("saved-refresh", storage.getRefreshToken())
                assertFalse(storage.clearTokensCalled)
                fail = false
                val restored = assertIs<ch.hslu.blebooking.mobile.model.SessionRestoreResult.Authenticated>(core.restoreSession())
                assertEquals("Alice", restored.profile.firstName)
                assertEquals("new-refresh", storage.getRefreshToken())
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun restoreSessionWithRevokedRefreshTokenClearsCredentials() = runTest {
        val storage = FakeTokenStorage(refreshToken = "revoked")
        val client = HttpClientFactory.create(MockEngine {
            respond("Unauthorized", HttpStatusCode.Unauthorized)
        }, "https://mock.test/", storage)
        try {
            val core = ch.hslu.blebooking.mobile.application.ServiceFacade(storage, BackendApiClient(client))
            assertEquals(ch.hslu.blebooking.mobile.model.SessionRestoreResult.Unauthenticated, core.restoreSession())
            assertNull(storage.getRefreshToken())
            assertNull(storage.getAccessToken())
        } finally {
            client.close()
        }
    }


    @Test
    fun logoutAndLoginUseFreshCredentialsOnTheSameClient() = runTest {
        val storage = FakeTokenStorage()
        val headers = mutableListOf<String?>()
        var loginCount = 0
        val engine = MockEngine { request ->
            val body = when (request.url.encodedPath) {
                "/auth/login" -> {
                    loginCount++
                    """{"accessToken":"access-$loginCount","refreshToken":"refresh-$loginCount"}"""
                }
                "/auth/logout" -> ""
                else -> {
                    headers.add(request.headers[HttpHeaders.Authorization])
                    "{}"
                }
            }
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory.create(engine, "https://mock.test/", storage)
        val auth = ch.hslu.blebooking.mobile.application.AuthService(storage, BackendApiClient(client))
        try {
            auth.login("a@example.com", "password")
            client.get("users/me")
            auth.logout()
            client.get("users/me")
            auth.login("b@example.com", "password")
            client.get("users/me")
            assertEquals(listOf("Bearer access-1", null, "Bearer access-2"), headers)
        } finally {
            client.close()
        }
    }

    private class FakeTokenStorage(
        private var accessToken: String? = null,
        private var refreshToken: String? = null
    ) : TokenStorage {
        var clearTokensCalled = false
            private set
        var storeTokensCalled = false
            private set

        override fun getAccessToken(): String? = accessToken

        override fun getRefreshToken(): String? = refreshToken

        override fun storeTokens(accessToken: String, refreshToken: String) {
            this.accessToken = accessToken
            this.refreshToken = refreshToken
            this.storeTokensCalled = true
        }

        override fun clearTokens() {
            this.accessToken = null
            this.refreshToken = null
            this.clearTokensCalled = true
        }
    }

    @Test
    fun authenticatedRequestIncludesBearerToken() = runTest {
        val storage = FakeTokenStorage(
            accessToken = "initial-access-token",
            refreshToken = "initial-refresh-token"
        )

        var capturedAuthHeader: String? = null

        val engine = MockEngine { request ->
            capturedAuthHeader = request.headers[HttpHeaders.Authorization]
            respond(
                content = """{"status":"ok"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClientFactory.create(
            engine = engine,
            baseUrl = "https://mock.test/",
            tokenStorage = storage
        )

        try {
            val response = client.get("users/me")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Bearer initial-access-token", capturedAuthHeader)
        } finally {
            client.close()
        }
    }

    @Test
    fun publicEndpointsDoNotIncludeBearerToken() = runTest {
        val storage = FakeTokenStorage(
            accessToken = "valid-token",
            refreshToken = "valid-refresh"
        )

        val capturedHeaders = mutableListOf<String?>()

        val engine = MockEngine { request ->
            capturedHeaders.add(request.headers[HttpHeaders.Authorization])
            respond(
                content = """{"status":"ok"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClientFactory.create(
            engine = engine,
            baseUrl = "https://mock.test/",
            tokenStorage = storage
        )

        try {
            client.post("auth/login")
            client.post("auth/refresh")

            assertEquals(2, capturedHeaders.size)
            assertNull(capturedHeaders[0], "auth/login must not include Authorization header")
            assertNull(capturedHeaders[1], "auth/refresh must not include Authorization header")
        } finally {
            client.close()
        }
    }

    @Test
    fun unintendedHostsDoNotReceiveBearerToken() = runTest {
        val storage = FakeTokenStorage(
            accessToken = "secret-token",
            refreshToken = "secret-refresh"
        )

        var capturedAuthHeader: String? = "preset"

        val engine = MockEngine { request ->
            capturedAuthHeader = request.headers[HttpHeaders.Authorization]
            respond(
                content = """{"status":"ok"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClientFactory.create(
            engine = engine,
            baseUrl = "https://mock.test/",
            tokenStorage = storage
        )

        try {
            client.get("https://unintended-host.com/data")
            assertNull(capturedAuthHeader, "Requests to unintended hosts must not include Authorization header")
        } finally {
            client.close()
        }
    }

    @Test
    fun on401AutomaticTokenRefreshAndRetrySucceeds() = runTest {
        val storage = FakeTokenStorage(
            accessToken = "expired-token",
            refreshToken = "valid-refresh-token"
        )

        var requestCount = 0
        var refreshRequestSeen = false

        val engine = MockEngine { request ->
            val path = request.url.encodedPath.trimStart('/')
            if (path == "auth/refresh") {
                refreshRequestSeen = true
                val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
                assertEquals("valid-refresh-token", body["refreshToken"]?.jsonPrimitive?.content)
                respond(
                    content = """{"accessToken":"new-access-token","refreshToken":"new-refresh-token"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                requestCount++
                if (requestCount == 1) {
                    // Prima chiamata con token scaduto
                    assertEquals("Bearer expired-token", request.headers[HttpHeaders.Authorization])
                    respond(
                        content = "Unauthorized",
                        status = HttpStatusCode.Unauthorized
                    )
                } else {
                    // Retry dopo refresh riuscito
                    assertEquals("Bearer new-access-token", request.headers[HttpHeaders.Authorization])
                    respond(
                        content = """{"id":1,"email":"test@example.com"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
        }

        val client = HttpClientFactory.create(
            engine = engine,
            baseUrl = "https://mock.test/",
            tokenStorage = storage
        )

        try {
            val response = client.get("users/me")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(refreshRequestSeen, "auth/refresh should have been called")
            assertEquals(2, requestCount, "users/me should have been requested twice (initial + retry)")
            assertTrue(storage.storeTokensCalled, "New tokens should be saved to TokenStorage")
            assertEquals("new-access-token", storage.getAccessToken())
            assertEquals("new-refresh-token", storage.getRefreshToken())
        } finally {
            client.close()
        }
    }

    @Test
    fun on401IfRefreshFailsTokensAreCleared() = runTest {
        val storage = FakeTokenStorage(
            accessToken = "expired-token",
            refreshToken = "invalid-refresh-token"
        )

        val engine = MockEngine { request ->
            val path = request.url.encodedPath.trimStart('/')
            if (path == "auth/refresh") {
                respond(
                    content = "Invalid refresh token",
                    status = HttpStatusCode.Unauthorized
                )
            } else {
                respond(
                    content = "Unauthorized",
                    status = HttpStatusCode.Unauthorized
                )
            }
        }

        val client = HttpClientFactory.create(
            engine = engine,
            baseUrl = "https://mock.test/",
            tokenStorage = storage
        )

        try {
            val response = client.get("users/me")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertTrue(storage.clearTokensCalled, "TokenStorage.clearTokens() should be called when refresh fails")
            assertNull(storage.getAccessToken())
            assertNull(storage.getRefreshToken())
        } finally {
            client.close()
        }
    }
}
