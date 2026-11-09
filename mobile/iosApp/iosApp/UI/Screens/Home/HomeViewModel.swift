import Foundation
import Observation
import SharedLogic

@MainActor
@Observable
final class HomeViewModel {
    private let mobileCoreApi: MobileCoreApi
    @ObservationIgnored private var logoutTask: Task<Void, Never>?

    private(set) var isLoading = false
    private(set) var errorMessage: String?
    private(set) var didLogout = false

    init(mobileCoreApi: MobileCoreApi) {
        self.mobileCoreApi = mobileCoreApi
    }

    func logout() {
        guard !isLoading else { return }

        errorMessage = nil
        didLogout = false
        isLoading = true

        logoutTask = Task { [weak self] in
            guard let self else { return }
            defer {
                self.isLoading = false
                self.logoutTask = nil
            }

            do {
                try Task.checkCancellation()
                try await self.mobileCoreApi.logout()
                try Task.checkCancellation()

                self.didLogout = true
            } catch is CancellationError {
                // Task cancelled
            } catch {
                guard !Task.isCancelled else { return }

                let nsError = error as NSError
                let coreError = (error as? MobileCoreException)
                    ?? (nsError.userInfo["KotlinException"] as? MobileCoreException)

                if let backendMessage = coreError?.message, !backendMessage.isEmpty {
                    self.errorMessage = backendMessage
                } else if let codeMessage = coreError?.code.message, !codeMessage.isEmpty {
                    self.errorMessage = codeMessage
                } else if !nsError.localizedDescription.isEmpty,
                          !nsError.localizedDescription.contains("KotlinException error") {
                    self.errorMessage = nsError.localizedDescription
                } else {
                    self.errorMessage = String(localized: "logout_error")
                }
            }
        }
    }

    func cancelLogout() {
        logoutTask?.cancel()
    }
}
