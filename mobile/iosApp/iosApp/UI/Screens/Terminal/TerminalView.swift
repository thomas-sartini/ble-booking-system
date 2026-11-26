import SwiftUI

@MainActor
struct TerminalView: View {
    let terminal: DiscoveredTerminal
    let discovery: TerminalDiscoveryManager

    @Environment(\.scenePhase) private var scenePhase
    @State private var lastRSSI: Int?

    private var signal: Int? { lastRSSI ?? terminal.rssi }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                header

                VStack(alignment: .leading, spacing: 16) {
                    HStack(spacing: 12) {
                        Text("terminal_last_signal")
                            .font(.headline)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        switch discovery.signalCheck {
                        case .refreshing:
                            ProgressView()
                                .accessibilityLabel(Text("terminal_refresh_signal"))
                        case .notDetected:
                            Text("terminal_signal_not_detected")
                                .font(.subheadline)
                                .foregroundStyle(BLETheme.secondaryText)
                        default:
                            SignalStrength(rssi: signal)
                        }
                    }

                    if discovery.signalCheck == .failed {
                        Text("terminal_signal_refresh_failed")
                            .font(.subheadline)
                            .foregroundStyle(BLETheme.error.text)
                    } else if discovery.signalCheck == .notDetected {
                        Text("terminal_signal_not_detected_hint")
                            .font(.subheadline)
                            .foregroundStyle(BLETheme.secondaryText)
                    }

                    Divider()

                    LabeledContent("terminal_connection") {
                        Text("terminal_not_connected")
                            .foregroundStyle(BLETheme.secondaryText)
                    }
                }
                .padding(18)
                .background(BLETheme.surface, in: RoundedRectangle(cornerRadius: 18))

                DisclosureGroup("terminal_device_details") {
                    VStack(alignment: .leading, spacing: 12) {
                        deviceDetail("terminal_identifier", value: terminal.id.uuidString)
                        deviceDetail("terminal_service", value: BleScanner.serviceUUID)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.top, 12)
                }
                .padding(18)
                .background(BLETheme.surface, in: RoundedRectangle(cornerRadius: 18))
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 20)
        }
        .foregroundStyle(BLETheme.text)
        .background(BLETheme.background.ignoresSafeArea())
        .navigationTitle(Text("terminal_title"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    discovery.refreshSignal(for: terminal)
                } label: {
                    Label("terminal_refresh_signal", systemImage: "arrow.clockwise")
                }
                .disabled(discovery.signalCheck == .refreshing || scenePhase != .active)
            }
        }
        .onChange(of: discovery.signalCheck) { _, check in
            if case .received(let rssi) = check { lastRSSI = rssi }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase != .active { discovery.cancelSignalRefresh() }
        }
        .onDisappear { discovery.clearSignalCheck() }
    }

    private var header: some View {
        VStack(spacing: 12) {
            Image(systemName: "antenna.radiowaves.left.and.right")
                .font(.largeTitle)
                .foregroundStyle(BLETheme.accent)
                .frame(width: 76, height: 76)
                .background(BLETheme.surfaceVariant, in: RoundedRectangle(cornerRadius: 22))
                .accessibilityHidden(true)

            Group {
                if let name = terminal.name {
                    Text(verbatim: name)
                } else {
                    Text("terminal_unnamed")
                }
            }
            .font(.title2.bold())
            .accessibilityAddTraits(.isHeader)
            .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }

    private func deviceDetail(_ label: LocalizedStringKey, value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundStyle(BLETheme.secondaryText)
            Text(verbatim: value)
                .font(.subheadline.monospaced())
                .textSelection(.enabled)
        }
        .accessibilityElement(children: .combine)
    }
}
