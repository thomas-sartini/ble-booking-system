
package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.communication.CommunicationGateway
import ch.hslu.blebooking.mobile.model.AccessConfirmation
import ch.hslu.blebooking.mobile.model.ResourceAccessRequest
import ch.hslu.blebooking.mobile.model.ResourceAccessResponse

/**
 * Handles resource-access application operations.
 *
 * Coordinates resource-access requests, confirmations,
 * and check-out operations.
 */
internal class AccessService(
    private val communicationGateway: CommunicationGateway
) {

    suspend fun requestAccess(
        request: ResourceAccessRequest
    ): ResourceAccessResponse {
        TODO("Not yet implemented")
    }

    suspend fun confirmAccess(
        confirmation: AccessConfirmation
    ) {
        TODO("Not yet implemented")
    }

    suspend fun checkOut(resourceId: String) {
        TODO("Not yet implemented")
    }
}
