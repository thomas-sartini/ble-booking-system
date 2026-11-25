package ch.hslu.blebooking.mobile.model

import ch.hslu.blebooking.mobile.model.error.MobileCoreException

sealed interface SessionRestoreResult {
    data class Authenticated(val profile: UserProfile) : SessionRestoreResult
    data object Unauthenticated : SessionRestoreResult
    data class Failed(val error: MobileCoreException) : SessionRestoreResult
}
