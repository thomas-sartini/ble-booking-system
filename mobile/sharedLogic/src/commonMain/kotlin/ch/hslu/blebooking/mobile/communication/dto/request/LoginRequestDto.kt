package ch.hslu.blebooking.mobile.communication.dto.request


import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val identifier: String,
    val password: String
)