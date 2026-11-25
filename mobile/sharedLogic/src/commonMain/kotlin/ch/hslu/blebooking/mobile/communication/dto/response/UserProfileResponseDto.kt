package ch.hslu.blebooking.mobile.communication.dto.response

import kotlinx.serialization.Serializable

@Serializable
internal data class UserProfileResponseDto(
    val id: Long? = null,
    val email: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val role: String
)
