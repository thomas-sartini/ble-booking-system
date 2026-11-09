
package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.api.MobileCoreApi
import ch.hslu.blebooking.mobile.communication.networking.ApiClient
import ch.hslu.blebooking.mobile.token.TokenStorage
import ch.hslu.blebooking.mobile.model.AccessConfirmation
import ch.hslu.blebooking.mobile.model.Booking
import ch.hslu.blebooking.mobile.model.ResourceAccessRequest
import ch.hslu.blebooking.mobile.model.ResourceAccessResponse

/**
 * Internal implementation of [MobileCoreApi].
 *
 * Creates the application services internally and delegates
 * operations to them.
 *
 * Hides internal service orchestration from native
 * Android and iOS applications.
 *
 * Application and communication errors are propagated
 * to the caller through the public MobileCore API.
 */
internal class ServiceFacade(
    tokenStorage: TokenStorage,
    apiClient: ApiClient
) : MobileCoreApi {
    private val authService = AuthService(tokenStorage, apiClient)
    private val bookingService = BookingService(apiClient)
    private val accessService = AccessService(apiClient)

    override suspend fun login(
        identifier: String,
        password: String
    ) {
        authService.login(identifier, password)
    }

    override suspend fun logout() {
        authService.logout()
    }

    override suspend fun getActiveBookings(): List<Booking> =
        bookingService.getActiveBookings()

    override suspend fun requestAccess(
        request: ResourceAccessRequest
    ): ResourceAccessResponse =
        accessService.requestAccess(request)

    override suspend fun confirmAccess(
        confirmation: AccessConfirmation
    ) {
        accessService.confirmAccess(confirmation)
    }

    override suspend fun checkOut(
        resourceId: String
    ) {
        accessService.checkOut(resourceId)
    }
}
