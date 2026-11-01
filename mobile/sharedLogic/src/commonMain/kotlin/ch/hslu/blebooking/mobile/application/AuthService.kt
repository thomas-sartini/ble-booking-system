
package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.communication.CommunicationGateway
import ch.hslu.blebooking.mobile.model.error.MobileCoreErrorCode
import ch.hslu.blebooking.mobile.model.error.MobileCoreException

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
        if(!identifier.isValidEmail()){
            throw MobileCoreException(MobileCoreErrorCode.VALIDATION_ERROR, "Invalid email")
        }
        communicationGateway.login(identifier, password)
    }

    suspend fun logout() {
        TODO("Not yet implemented")
    }

    private fun String.isValidEmail(): Boolean {
        if (this.isNullOrBlank()) return false
        val emailRegex = Regex(
            """^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"""
        )

        return emailRegex.matches(this)
    }
}
