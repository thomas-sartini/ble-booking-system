enum MessageType {
    case success
    case error
    case warning
}

/// Text is resolved through localization before creating the message.
struct MessageData {
    let text: String
    var type: MessageType = .success
}
