
package ch.hslu.blebooking.mobile.communication

import ch.hslu.blebooking.mobile.communication.dto.RefreshTokenRequestDto
import ch.hslu.blebooking.mobile.model.AccessConfirmation
import ch.hslu.blebooking.mobile.model.Booking
import ch.hslu.blebooking.mobile.model.ResourceAccessRequest
import ch.hslu.blebooking.mobile.model.ResourceAccessResponse
import ch.hslu.blebooking.mobile.model.error.MobileCoreException

/**
 * Internal contract for communication with the backend.
 *
 * Abstracts backend operations from the application services,
 * allowing communication implementations to be replaced
 * or mocked during testing.
 *
 * Uses platform-independent application models and hides
 * HTTP, serialization, and networking details.
 *
 * Operations complete normally on success and throw
 * [MobileCoreException] on application or communication failures.
 */
internal interface CommunicationGateway {

    /**
     * Authenticates a user against the backend.
     *
     * @param identifier The user's authentication identifier.
     * @param password The user's password.
     * @throws MobileCoreException If authentication fails
     * or a communication error occurs.
     */
    suspend fun login(
        identifier: String,
        password: String
    )

    /**
     * Requests termination of the current backend session.
     *
     * @throws MobileCoreException If the backend operation fails.
     */
    suspend fun logout()

    /**
     * Retrieves active bookings for the authenticated user.
     *
     * @return A list of active [Booking] objects,
     * or an empty list if no active bookings exist.
     * @throws MobileCoreException If the operation fails.
     */
    suspend fun getActiveBookings(): List<Booking>

    /**
     * Requests resource-access authorization from the backend.
     *
     * @param request The resource-access request.
     * @return The response containing the information
     * required for the resource-access operation.
     * @throws MobileCoreException If authorization fails
     * or a communication error occurs.
     */
    suspend fun requestAccess(
        request: ResourceAccessRequest
    ): ResourceAccessResponse

    /**
     * Sends the result of a resource-access operation
     * to the backend.
     *
     * @param confirmation The access confirmation data.
     * @throws MobileCoreException If the confirmation
     * is rejected or cannot be completed.
     */
    suspend fun confirmAccess(
        confirmation: AccessConfirmation
    )

    /**
     * Requests check-out of the specified resource
     * from the backend.
     *
     * @param resourceId The identifier of the resource.
     * @throws MobileCoreException If check-out is rejected
     * or a communication error occurs.
     */
    suspend fun checkOut(
        resourceId: String
    )

}
