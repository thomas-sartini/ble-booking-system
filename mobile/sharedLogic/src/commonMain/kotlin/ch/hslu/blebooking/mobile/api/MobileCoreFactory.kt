
package ch.hslu.blebooking.mobile.api

import ch.hslu.blebooking.mobile.application.AccessService
import ch.hslu.blebooking.mobile.application.AuthService
import ch.hslu.blebooking.mobile.application.BookingService
import ch.hslu.blebooking.mobile.application.ServiceFacade
import ch.hslu.blebooking.mobile.communication.BackendCommunication
import ch.hslu.blebooking.mobile.communication.CommunicationGateway
import ch.hslu.blebooking.mobile.token.TokenManager

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
     * @return A configured [MobileCoreApi] instance.
     */
    fun create(secureStorage: SecureStorage): MobileCoreApi {

        val tokenManager = TokenManager(secureStorage)

        val communicationGateway: CommunicationGateway =
            BackendCommunication(tokenManager)

        return ServiceFacade(
            authService = AuthService(communicationGateway),
            bookingService = BookingService(communicationGateway),
            accessService = AccessService(communicationGateway)
        )
    }
}
