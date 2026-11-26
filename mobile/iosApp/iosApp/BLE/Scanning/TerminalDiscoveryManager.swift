import Foundation
import Observation

@MainActor
@Observable
final class TerminalDiscoveryManager {
    enum SignalCheck: Equatable {
        case idle
        case refreshing
        case received(Int)
        case notDetected
        case failed
    }

    private enum Activity: Equatable {
        case idle
        case discovering
        case checkingSignal(UUID)
    }

    private struct TerminalObservation {
        let terminal: DiscoveredTerminal
        let lastSeen: ContinuousClock.Instant
    }

    private(set) var availability: BleAvailability = .permissionRequired
    private(set) var terminals: [DiscoveredTerminal] = []
    private(set) var scanFailed = false
    private(set) var signalCheck: SignalCheck = .idle
    var isScanning: Bool { activity == .discovering }

    private let scanner: any TerminalScanner
    private let clock = ContinuousClock()
    private var activity: Activity = .idle
    @ObservationIgnored private var observations: [UUID: TerminalObservation] = [:]
    @ObservationIgnored private var monitor: Task<Void, Never>?

    init(scanner: any TerminalScanner) {
        self.scanner = scanner
        scanner.onAvailabilityChanged = { [weak self] in self?.availabilityChanged($0) }
        scanner.onTerminalDiscovered = { [weak self] in self?.observe($0) }
    }

    func prepare(requestPermission: Bool = false) {
        scanner.prepare(requestPermission: requestPermission)
    }

    func startDiscovery() {
        guard activity == .idle, availability == .ready else { return }
        observations.removeAll()
        terminals = []
        scanFailed = !scanner.startScan()
        guard !scanFailed else { return }

        activity = .discovering
        monitor = Task { [weak self] in
            while !Task.isCancelled {
                do {
                    try await Task.sleep(for: .seconds(1))
                } catch {
                    return
                }
                guard let self, !Task.isCancelled else { return }
                let now = clock.now
                observations = observations.filter { $0.value.lastSeen.duration(to: now) < .seconds(6) }
                publishTerminals()
            }
        }
    }

    func stopDiscovery() {
        guard activity == .discovering else { return }
        stopScan()
    }

    func refreshSignal(for terminal: DiscoveredTerminal) {
        guard activity == .idle else { return }
        guard availability == .ready, scanner.startScan() else {
            signalCheck = .failed
            return
        }

        activity = .checkingSignal(terminal.id)
        signalCheck = .refreshing
        monitor = Task { [weak self] in
            do {
                try await Task.sleep(for: .seconds(5))
            } catch {
                return
            }
            guard let self, !Task.isCancelled else { return }
            signalCheck = .notDetected
            stopScan()
        }
    }

    func cancelSignalRefresh() {
        guard case .checkingSignal = activity else { return }
        stopScan()
        signalCheck = .idle
    }

    func clearSignalCheck() {
        cancelSignalRefresh()
        signalCheck = .idle
    }

    private func availabilityChanged(_ availability: BleAvailability) {
        self.availability = availability
        guard availability != .ready else { return }
        observations.removeAll()
        terminals = []
        scanFailed = false
        if case .checkingSignal = activity {
            signalCheck = .failed
        }
        stopScan()
    }

    private func observe(_ terminal: DiscoveredTerminal) {
        switch activity {
        case .discovering:
            let previous = observations[terminal.id]?.terminal
            let updated = DiscoveredTerminal(
                id: terminal.id,
                name: terminal.name ?? previous?.name,
                rssi: terminal.rssi ?? previous?.rssi
            )
            observations[terminal.id] = TerminalObservation(terminal: updated, lastSeen: clock.now)
            publishTerminals()
        case .checkingSignal(let id) where terminal.id == id:
            guard let rssi = terminal.rssi else { return }
            signalCheck = .received(rssi)
            stopScan()
        default:
            break
        }
    }

    private func publishTerminals() {
        let sorted = observations.values.map(\.terminal).sorted { lhs, rhs in
            let lhsRSSI = lhs.rssi ?? Int.min
            let rhsRSSI = rhs.rssi ?? Int.min
            if lhsRSSI != rhsRSSI { return lhsRSSI > rhsRSSI }
            return lhs.id.uuidString < rhs.id.uuidString
        }
        if sorted != terminals { terminals = sorted }
    }

    private func stopScan() {
        guard activity != .idle else { return }
        activity = .idle
        monitor?.cancel()
        monitor = nil
        scanner.stopScan()
    }
}
