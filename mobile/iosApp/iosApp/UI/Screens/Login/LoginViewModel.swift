import Foundation
import Observation
import SharedLogic

@MainActor
@Observable
final class LoginViewModel {
    private let mobileCoreApi: MobileCoreApi
    @ObservationIgnored private var signInTask: Task<Void, Never>?

    var email = ""
    var password = ""

    private(set) var isLoading = false
    private(set) var errorMessage: String?
    private(set) var didSignIn = false

    private var identifier: String {
        email.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var canSignIn: Bool {
        !identifier.isEmpty && !password.isEmpty && !isLoading
    }

    init(mobileCoreApi: MobileCoreApi) {
        self.mobileCoreApi = mobileCoreApi
    }

    func signIn() {
        guard !isLoading else { return }

        errorMessage = nil
        didSignIn = false

        guard !identifier.isEmpty, !password.isEmpty else {
            errorMessage = String(localized: "login_error_required_fields")
            return
        }

        let identifier = identifier
        let password = password
        isLoading = true
        signInTask = Task { [weak self] in
            guard let self else { return }
            defer {
                self.isLoading = false
                self.signInTask = nil
            }

            do {
                try Task.checkCancellation()
                try await self.mobileCoreApi.login(
                    identifier: identifier,
                    password: password
                )
                try Task.checkCancellation()

                self.password = ""
                self.didSignIn = true
            } catch is CancellationError {
                // Leaving the screen is not a login failure.
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
                    self.errorMessage = String(localized: "login_error_failed")
                }
            }
        }
    }

    func cancelSignIn() {
        signInTask?.cancel()
        password = ""
    }
}
