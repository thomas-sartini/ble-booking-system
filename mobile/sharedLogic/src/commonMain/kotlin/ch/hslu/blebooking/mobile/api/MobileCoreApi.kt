
package ch.hslu.blebooking.mobile.api

import ch.hslu.blebooking.mobile.model.Booking
import ch.hslu.blebooking.mobile.model.ResourceAccessRequest
import ch.hslu.blebooking.mobile.model.ResourceAccessResponse
import ch.hslu.blebooking.mobile.model.AccessConfirmation
import ch.hslu.blebooking.mobile.model.error.MobileCoreException

/**
 * Public API exposed by the shared Mobile Core to native
 * Android and iOS applications.
 *
 * Provides authentication, booking, and resource-access
 * operations using platform-independent application models.
 *
 * Backend communication, session management, and token
 * handling are encapsulated within the Mobile Core.
 *
 * BLE communication is managed by the native applications.
 *
 * Operations complete normally on success and throw
 * [MobileCoreException] on expected application or
 * communication failures.
 */
interface MobileCoreApi {

    /**
     * Authenticates a user using the provided credentials.
     *
     * Authentication tokens are managed internally and
     * are not exposed to native applications.
     *
     * @param identifier The user's authentication identifier.
     * @param password The user's password.
     * @throws MobileCoreException If authentication fails
     * or a communication error occurs.
     */
    @Throws(MobileCoreException::class)
    suspend fun login(
        identifier: String,
        password: String
    )

    /**
     * Terminates the current authenticated session.
     *
     * Coordinates backend session termination and
     * local credential cleanup.
     *
     * @throws MobileCoreException If the operation fails.
     */
    @Throws(MobileCoreException::class)
    suspend fun logout()

    /**
     * Retrieves the active bookings of the current user.
     *
     * @return A list of active [Booking] objects,
     * or an empty list if no active bookings exist.
     * @throws MobileCoreException If the operation fails.
     */
    @Throws(MobileCoreException::class)
    suspend fun getActiveBookings(): List<Booking>

    /**
     * Requests authorization to access a resource.
     *
     * @param request The resource-access request.
     * @return The resource-access response.
     * @throws MobileCoreException If the request is
     * rejected or a communication error occurs.
     */
    @Throws(MobileCoreException::class)
    suspend fun requestAccess(
        request: ResourceAccessRequest
    ): ResourceAccessResponse

    /**
     * Confirms the outcome of a resource-access operation.
     *
     * @param confirmation The access confirmation data.
     * @throws MobileCoreException If the confirmation
     * is rejected or cannot be completed.
     */
    @Throws(MobileCoreException::class)
    suspend fun confirmAccess(
        confirmation: AccessConfirmation
    )

    /**
     * Performs check-out for the specified resource.
     *
     * @param resourceId The identifier of the resource.
     * @throws MobileCoreException If check-out is
     * rejected or cannot be completed.
     */
    @Throws(MobileCoreException::class)
    suspend fun checkOut(
        resourceId: String
    )
}
