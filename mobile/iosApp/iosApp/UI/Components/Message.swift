import SwiftUI

/// An inline status message. Text and description must already be localized.
struct Message: View {
    let text: String
    let type: MessageType
    var description: String? = nil

    private var accent: Color {
        switch type {
        case .success: .green
        case .error: .red
        case .warning: .orange
        }
    }

    private var symbol: String {
        switch type {
        case .success: "checkmark.circle.fill"
        case .error: "exclamationmark.circle.fill"
        case .warning: "exclamationmark.triangle.fill"
        }
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: symbol)
                .font(.body.weight(.semibold))
                .foregroundStyle(accent)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 4) {
                Text(verbatim: text)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.primary)

                if let description {
                    Text(verbatim: description)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(16)
        .background(
            accent.opacity(0.12),
            in: RoundedRectangle(cornerRadius: 12, style: .continuous)
        )
        .accessibilityElement(children: .combine)
    }
}

#Preview("Messages — Light") {
    Message(
        text: String(localized: "home_login_success"),
        type: .success
    )
        .padding()
        .preferredColorScheme(.light)
}

#Preview("Messages — Dark") {
    Message(
        text: String(localized: "login_error_failed"),
        type: .error
    )
        .padding()
        .preferredColorScheme(.dark)
}

