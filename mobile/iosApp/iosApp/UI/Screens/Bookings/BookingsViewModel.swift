import Foundation
import Observation
import SharedLogic

@MainActor
@Observable
final class BookingsViewModel {
    enum State {
        case loading
        case ready([Booking])
        case failed(sessionExpired: Bool)
    }

    private let mobileCoreApi: MobileCoreApi
    private(set) var state: State = .loading
    private(set) var isLoading = false

    var featuredBooking: Booking? {
        guard case .ready(let bookings) = state else { return nil }
        return bookings.first { $0.status == .checkedIn } ?? bookings.first
    }

    init(mobileCoreApi: MobileCoreApi) {
        self.mobileCoreApi = mobileCoreApi
    }

    func loadBookings() async {
        guard !isLoading, !Task.isCancelled else { return }
        let previousState = state
        state = .loading
        isLoading = true
        defer { isLoading = false }

        do {
            let bookings = try await mobileCoreApi.getActiveBookings()
            try Task.checkCancellation()
            state = .ready(bookings.sorted { $0.startTime.compareTo(other: $1.startTime) < 0 })
        } catch is CancellationError {
            state = previousState
        } catch {
            guard !Task.isCancelled else {
                state = previousState
                return
            }
            let coreError = (error as? MobileCoreException)
                ?? ((error as NSError).userInfo["KotlinException"] as? MobileCoreException)
            state = .failed(sessionExpired: coreError?.code == .unauthorized)
        }
    }
}
