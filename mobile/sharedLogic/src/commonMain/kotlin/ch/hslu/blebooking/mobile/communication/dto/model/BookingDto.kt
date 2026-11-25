package ch.hslu.blebooking.mobile.communication.dto.model

import kotlinx.serialization.Serializable

@Serializable
internal data class BookingDto(
    val id: Long,
    val resource: BookingResourceDto,
    val start: String,
    val end: String,
    val status: String,
)

@Serializable
internal data class BookingResourceDto(
    val id: Long,
    val name: String,
    val position: String? = null,
)
