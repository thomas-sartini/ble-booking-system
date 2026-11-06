package ch.hslu.blebooking.mobile.communication.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenResponseDto(
    val accessToken: String,
    val refreshToken: String
)
