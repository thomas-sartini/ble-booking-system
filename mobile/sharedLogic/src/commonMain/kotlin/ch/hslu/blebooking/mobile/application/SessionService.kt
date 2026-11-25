package ch.hslu.blebooking.mobile.application

import ch.hslu.blebooking.mobile.model.SessionRestoreResult
import ch.hslu.blebooking.mobile.model.error.MobileCoreErrorCode
import ch.hslu.blebooking.mobile.model.error.MobileCoreException
import ch.hslu.blebooking.mobile.token.TokenStorage
import kotlin.coroutines.cancellation.CancellationException

internal class SessionService(
    private val storage: TokenStorage,
    private val auth: AuthService,
    private val users: UserService,
) {
    suspend fun restoreSession(): SessionRestoreResult {
        return try {
            if (storage.getRefreshToken().isNullOrBlank()) {
                auth.clearTokens()
                SessionRestoreResult.Unauthenticated
            } else {
                SessionRestoreResult.Authenticated(users.getUserProfile())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: MobileCoreException) {
            if (e.code == MobileCoreErrorCode.UNAUTHORIZED) {
                try {
                    auth.clearTokens()
                    SessionRestoreResult.Unauthenticated
                } catch (cleanup: MobileCoreException) {
                    SessionRestoreResult.Failed(cleanup)
                }
            } else {
                SessionRestoreResult.Failed(e)
            }
        }
    }
}
