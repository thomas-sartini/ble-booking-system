package ch.hslu.blebooking.mobile.model

import kotlin.time.Instant

data class Booking(
    val id: String,
    val resourceId: String,
    val startTime: Instant,
    val endTime: Instant,
    val status: BookingStatus
)