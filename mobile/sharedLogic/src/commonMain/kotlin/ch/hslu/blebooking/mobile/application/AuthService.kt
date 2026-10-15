
package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.communication.CommunicationGateway

/**
 * Handles authentication-related application operations.
 *
 * Coordinates authentication and session termination
 * through the communication gateway.
 */
internal class AuthService(
    private val communicationGateway: CommunicationGateway
) {

    suspend fun login(
        identifier: String,
        password: String
    ) {
        TODO("Not yet implemented")
    }

    suspend fun logout() {
        TODO("Not yet implemented")
    }
}
