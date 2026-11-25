import SwiftUI

/// An inline status message. Text and description must already be localized.
struct Message: View {
    let text: String
    let type: MessageType
    var description: String? = nil

    private var colors: BLETheme.StatusColors {
        switch type {
        case .success: BLETheme.success
        case .error: BLETheme.error
        case .warning: BLETheme.warning
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
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 4) {
                Text(verbatim: text)
                    .font(.subheadline.weight(.semibold))

                if let description {
                    Text(verbatim: description)
                        .font(.subheadline)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .foregroundStyle(colors.text)
        .padding(14)
        .background(colors.background, in: RoundedRectangle(cornerRadius: 8))
        .overlay {
            RoundedRectangle(cornerRadius: 8)
                .strokeBorder(colors.accent.opacity(type == .error ? 0.6 : 0.35))
        }
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

