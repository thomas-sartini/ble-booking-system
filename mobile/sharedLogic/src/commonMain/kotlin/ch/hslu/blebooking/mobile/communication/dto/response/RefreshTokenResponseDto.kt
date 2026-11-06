package ch.hslu.blebooking.mobile.communication.dto.response

import kotlinx.serialization.Serializable

@Serializable
internal data class RefreshTokenResponseDto(
    val accessToken: String,
    val refreshToken: String
)
