import SwiftUI

struct TerminalCard: View {
    let terminal: DiscoveredTerminal

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: "antenna.radiowaves.left.and.right")
                .foregroundStyle(BLETheme.accent)
                .frame(width: 44, height: 44)
                .background(BLETheme.surfaceVariant, in: RoundedRectangle(cornerRadius: 13))
                .accessibilityHidden(true)

            Group {
                if let name = terminal.name {
                    Text(verbatim: name)
                } else {
                    Text("terminal_unnamed")
                }
            }
            .font(.headline)
            .foregroundStyle(BLETheme.text)
            .frame(maxWidth: .infinity, alignment: .leading)

            SignalStrength(rssi: terminal.rssi)

            Image(systemName: "chevron.forward")
                .font(.caption.weight(.semibold))
                .foregroundStyle(BLETheme.secondaryText)
                .accessibilityHidden(true)
        }
        .padding(16)
        .background(BLETheme.surface, in: RoundedRectangle(cornerRadius: 18))
        .overlay {
            RoundedRectangle(cornerRadius: 18)
                .strokeBorder(BLETheme.outline.opacity(0.24))
        }
        .accessibilityElement(children: .combine)
    }
}
