package ch.hslu.blebooking.mobile.api

import ch.hslu.blebooking.mobile.application.AccessService
import ch.hslu.blebooking.mobile.application.AuthService
import ch.hslu.blebooking.mobile.application.BookingService
import ch.hslu.blebooking.mobile.application.ServiceFacade
import ch.hslu.blebooking.mobile.communication.BackendCommunication
import ch.hslu.blebooking.mobile.communication.CommunicationGateway
import ch.hslu.blebooking.mobile.communication.networking.BackendApiClient
import ch.hslu.blebooking.mobile.communication.networking.HttpClientFactory
import ch.hslu.blebooking.mobile.token.TokenManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine

/**
 * Factory responsible for creating and configuring
 * instances of [MobileCoreApi].
 *
 * Assembles all internal Mobile Core components while
 * exposing only the public API to native applications.
 *
 * Native applications must provide a platform-specific
 * implementation of [SecureStorage].
 */
object MobileCoreFactory {

    /**
     * Creates a fully configured Mobile Core instance.
     *
     * @param secureStorage Platform-specific secure storage
     * implementation provided by the native application.
     * @param baseUrl API base URL including the auth path, e.g. https://host/auth/.
     * @param httpClientEngine Optional platform-specific [HttpClientEngine]
     * (e.g. OkHttp on Android, Darwin on iOS) or mock engine for tests.
     * @return A configured [MobileCoreApi] instance.
     */
    fun create(
        secureStorage: SecureStorage,
        baseUrl: String,
        httpClientEngine: HttpClientEngine? = null
    ): MobileCoreApi {
        val httpClient: HttpClient = HttpClientFactory.create(
            engine = httpClientEngine,
            baseUrl = baseUrl.trimEnd('/') + "/"
        )
        val backendApiClient = BackendApiClient(httpClient)
        val tokenManager = TokenManager(secureStorage)
        val communicationGateway: CommunicationGateway =
            BackendCommunication(tokenManager, backendApiClient)

        return ServiceFacade(
            authService = AuthService(communicationGateway),
            bookingService = BookingService(communicationGateway),
            accessService = AccessService(communicationGateway)
        )
    }
}
