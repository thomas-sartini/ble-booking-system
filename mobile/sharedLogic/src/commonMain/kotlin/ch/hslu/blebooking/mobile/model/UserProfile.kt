package ch.hslu.blebooking.mobile.model

/**
 * Shared Mobile user-profile model containing user personal information.
 */
data class UserProfile(
    val firstName: String,
    val lastName: String,
    val username: String,
    val email: String,
    val role: Role
)
