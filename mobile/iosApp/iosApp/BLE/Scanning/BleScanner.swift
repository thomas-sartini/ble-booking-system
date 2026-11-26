import CoreBluetooth
import Foundation

enum BleAvailability {
    case permissionRequired
    case permissionDenied
    case initializing
    case poweredOff
    case unsupported
    case ready
}

@MainActor
protocol TerminalScanner: AnyObject {
    var onAvailabilityChanged: ((BleAvailability) -> Void)? { get set }
    var onTerminalDiscovered: ((DiscoveredTerminal) -> Void)? { get set }

    func prepare(requestPermission: Bool)
    func startScan() -> Bool
    func stopScan()
}

@MainActor
final class BleScanner: NSObject, @preconcurrency CBCentralManagerDelegate, TerminalScanner {
    static let serviceUUID = "FD698D10-C5C7-429B-9FB3-18A41DD73998"

    var onAvailabilityChanged: ((BleAvailability) -> Void)?
    var onTerminalDiscovered: ((DiscoveredTerminal) -> Void)?

    private var central: CBCentralManager?

    func prepare(requestPermission: Bool) {
        switch CBManager.authorization {
        case .notDetermined:
            guard requestPermission else {
                onAvailabilityChanged?(.permissionRequired)
                return
            }
        case .allowedAlways:
            break
        case .denied, .restricted:
            onAvailabilityChanged?(.permissionDenied)
            return
        @unknown default:
            onAvailabilityChanged?(.unsupported)
            return
        }

        if let central {
            centralManagerDidUpdateState(central)
        } else {
            onAvailabilityChanged?(.initializing)
            // The main queue keeps delegate callbacks and scan operations on the same actor.
            central = CBCentralManager(delegate: self, queue: .main, options: [
                CBCentralManagerOptionShowPowerAlertKey: false
            ])
        }
    }

    func startScan() -> Bool {
        guard let central, central.state == .poweredOn else { return false }
        central.scanForPeripherals(withServices: [CBUUID(string: Self.serviceUUID)], options: [
            CBCentralManagerScanOptionAllowDuplicatesKey: true
        ])
        return true
    }

    func stopScan() {
        if let central, central.state == .poweredOn, central.isScanning {
            central.stopScan()
        }
    }

    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        let availability: BleAvailability
        switch central.state {
        case .poweredOn: availability = .ready
        case .poweredOff: availability = .poweredOff
        case .unauthorized: availability = .permissionDenied
        case .unknown, .resetting: availability = .initializing
        case .unsupported: availability = .unsupported
        @unknown default: availability = .unsupported
        }
        onAvailabilityChanged?(availability)
    }

    func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi RSSI: NSNumber
    ) {
        guard central.state == .poweredOn, central.isScanning else { return }
        let name = (advertisementData[CBAdvertisementDataLocalNameKey] as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        // CoreBluetooth reports 127 when an RSSI measurement is unavailable.
        let rssi = RSSI.intValue
        onTerminalDiscovered?(DiscoveredTerminal(
            id: peripheral.identifier,
            name: name?.isEmpty == false ? name : nil,
            rssi: rssi == 127 ? nil : rssi
        ))
    }
}
