package ch.hslu.blebooking.mobile.model

data class AccessConfirmation(
    val bookingId: String,
    val signedReceipt: String
)