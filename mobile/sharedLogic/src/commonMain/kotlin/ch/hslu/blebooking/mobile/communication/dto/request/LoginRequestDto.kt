package ch.hslu.blebooking.mobile.communication.dto.request


import kotlinx.serialization.Serializable

@Serializable
internal data class LoginRequestDto(
    val identifier: String,
    val password: String
)