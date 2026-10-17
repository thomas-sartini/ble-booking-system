package ch.hslu.blebooking.mobile.model

data class ResourceAccessRequest(
    val resourceId: String,
    val bookingId: String?,
    val deviceId: String
)