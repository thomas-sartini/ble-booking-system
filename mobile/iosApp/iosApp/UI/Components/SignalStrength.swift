import SwiftUI

struct SignalStrength: View {
    let rssi: Int?

    private var barCount: Int {
        guard let rssi else { return 0 }
        switch rssi {
        case (-60)...: return 4
        case (-70)...: return 3
        case (-80)...: return 2
        default: return 1
        }
    }

    private var tint: Color {
        switch barCount {
        case 0: BLETheme.secondaryText
        case 1: BLETheme.warning.accent
        default: BLETheme.accent
        }
    }

    var body: some View {
        VStack(alignment: .trailing, spacing: 4) {
            HStack(alignment: .bottom, spacing: 3) {
                ForEach(0..<4) { index in
                    Capsule()
                        .fill(index < barCount ? tint : BLETheme.outline.opacity(0.25))
                        .frame(width: 5, height: CGFloat(index + 1) * 5)
                }
            }
            .accessibilityHidden(true)

            if let rssi {
                Text("\(rssi) dBm")
                    .monospacedDigit()
            } else {
                Text("terminal_signal_unknown")
            }
        }
        .foregroundStyle(tint)
        .font(.caption)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("terminal_last_signal"))
        .accessibilityValue(rssi.map { Text("\($0) dBm") } ?? Text("terminal_signal_unknown"))
    }
}
