import SwiftUI

@MainActor
struct ScanningView: View {
    let discovery: TerminalDiscoveryManager

    @Environment(\.scenePhase) private var scenePhase
    @State private var isVisible = false
    @State private var selectedTerminal: DiscoveredTerminal?

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                feedback

                ForEach(discovery.terminals) { terminal in
                    Button {
                        discovery.stopDiscovery()
                        selectedTerminal = terminal
                    } label: {
                        TerminalCard(terminal: terminal)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
        }
        .background(BLETheme.background.ignoresSafeArea())
        .navigationTitle(Text("scanning_title"))
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(item: $selectedTerminal) { terminal in
            TerminalView(terminal: terminal, discovery: discovery)
        }
        .onAppear {
            isVisible = true
            updateDiscovery()
        }
        .onDisappear {
            isVisible = false
            discovery.stopDiscovery()
        }
        .onChange(of: scenePhase) { _, _ in updateDiscovery() }
        .onChange(of: discovery.availability) { _, _ in updateDiscovery() }
        .onChange(of: selectedTerminal) { _, _ in updateDiscovery() }
    }

    @ViewBuilder
    private var feedback: some View {
        if discovery.isScanning {
            ProgressView("scanning_searching")
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
        } else if discovery.availability == .initializing {
            ProgressView("bluetooth_initializing")
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, 8)
        } else if discovery.availability != .ready {
            Message(text: String(localized: "scanning_unavailable"), type: .error)
        } else if discovery.scanFailed {
            VStack(spacing: 12) {
                Message(text: String(localized: "scanning_failed"), type: .error)
                Button("session_restore_retry", action: updateDiscovery)
                    .buttonStyle(.bordered)
            }
        }
    }

    private func updateDiscovery() {
        if isVisible, scenePhase == .active, selectedTerminal == nil {
            discovery.startDiscovery()
        } else {
            discovery.stopDiscovery()
        }
    }
}
