import Foundation

struct DiscoveredTerminal: Identifiable, Hashable {
    let id: UUID
    let name: String?
    let rssi: Int?
}
