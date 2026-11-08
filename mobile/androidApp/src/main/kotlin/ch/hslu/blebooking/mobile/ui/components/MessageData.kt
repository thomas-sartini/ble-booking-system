package ch.hslu.blebooking.mobile.ui.components

/** Plain data shared by screens; no Compose dependency. */
data class MessageData(
    val text: String,
    val type: MessageType = MessageType.Success,
)
