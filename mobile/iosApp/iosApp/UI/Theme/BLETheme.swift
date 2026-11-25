import SwiftUI
import UIKit

/// Semantic colors shared by the iOS screens and matched to Android's BLE theme.
enum BLETheme {
    static let background = adaptive(light: 0xF5FBF7, dark: 0x080D0C)
    static let surface = adaptive(light: 0xFFFFFF, dark: 0x101716)
    static let surfaceVariant = adaptive(light: 0xE3EFE8, dark: 0x1B2823)
    static let text = adaptive(light: 0x14211B, dark: 0xE8EFEC)
    static let secondaryText = adaptive(light: 0x52645A, dark: 0x9AA9A3)
    static let outline = adaptive(light: 0x73867B, dark: 0x35433D)

    static let accent = adaptive(light: 0x006C50, dark: 0x61DDB2)
    static let onAccent = adaptive(light: 0xFFFFFF, dark: 0x00382A)

    static let success = StatusColors(
        accent: accent,
        background: adaptive(light: 0xC0F0DB, dark: 0x1B3D2F),
        text: adaptive(light: 0x00513B, dark: 0x61DDB2)
    )
    static let error = StatusColors(
        accent: adaptive(light: 0xBA1A1A, dark: 0xFF6262),
        background: adaptive(light: 0xFFDAD6, dark: 0x6E1820),
        text: adaptive(light: 0x6E1820, dark: 0xFFDAD6)
    )
    static let warning = StatusColors(
        accent: adaptive(light: 0x795900, dark: 0xF2C66D),
        background: adaptive(light: 0xFFDEA0, dark: 0x594300),
        text: adaptive(light: 0x402D00, dark: 0xFFDEA0)
    )

    struct StatusColors {
        let accent: Color
        let background: Color
        let text: Color
    }

    private static func adaptive(light: UInt32, dark: UInt32) -> Color {
        Color(uiColor: UIColor { traits in
            let rgb = traits.userInterfaceStyle == .dark ? dark : light
            return UIColor(
                red: CGFloat((rgb >> 16) & 0xFF) / 255,
                green: CGFloat((rgb >> 8) & 0xFF) / 255,
                blue: CGFloat(rgb & 0xFF) / 255,
                alpha: 1
            )
        })
    }
}

struct BLEPrimaryButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundStyle(BLETheme.onAccent)
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, minHeight: 54)
            .background(BLETheme.accent, in: RoundedRectangle(cornerRadius: 12))
            .opacity(isEnabled ? (configuration.isPressed ? 0.85 : 1) : 0.55)
    }
}
