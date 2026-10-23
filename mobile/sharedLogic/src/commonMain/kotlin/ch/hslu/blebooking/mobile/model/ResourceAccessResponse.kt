package ch.hslu.blebooking.mobile.model

import kotlin.time.Instant

data class ResourceAccessResponse(
    val ticket: String,
    val expiresAt: Instant
)