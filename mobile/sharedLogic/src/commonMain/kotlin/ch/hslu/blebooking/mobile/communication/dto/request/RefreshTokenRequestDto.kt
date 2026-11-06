package ch.hslu.blebooking.mobile.communication.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenRequestDto(
    val refreshToken: String
)
