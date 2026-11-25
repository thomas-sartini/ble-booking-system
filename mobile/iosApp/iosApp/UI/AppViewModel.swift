import Foundation
import Observation
import SharedLogic

@MainActor
@Observable
final class AppViewModel {
    enum Screen {
        case loading
        case login(LoginViewModel)
        case home(HomeViewModel)
    }

    private let mobileCoreApi: MobileCoreApi
    private let secureStorage: NativeStorageDelegate
    private let refreshTokenKey = "refreshToken"

    private(set) var screen: Screen = .loading
    private(set) var messageData: MessageData?

    init(mobileCoreApi: MobileCoreApi, secureStorage: NativeStorageDelegate) {
        self.mobileCoreApi = mobileCoreApi
        self.secureStorage = secureStorage
    }

    func restoreSession() async {
        guard case .loading = screen, !Task.isCancelled else { return }

        let storedToken = secureStorage.get(key_: refreshTokenKey)
        guard storedToken.errorMessage == nil,
              let refreshToken = storedToken.value,
              !refreshToken.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            showLogin()
            return
        }

        do {
            // A protected request lets the shared core refresh the bearer token.
            let profile = try await mobileCoreApi.getUserProfile()
            guard !Task.isCancelled else { return }
            screen = .home(HomeViewModel(mobileCoreApi: mobileCoreApi, userProfile: profile))
        } catch {
            guard !Task.isCancelled else { return }
            _ = secureStorage.remove(key_: refreshTokenKey)
            showLogin()
        }
    }

    func didSignIn() {
        messageData = MessageData(text: String(localized: "home_login_success"))
        screen = .home(HomeViewModel(mobileCoreApi: mobileCoreApi))
    }

    func didLogout() {
        showLogin(message: MessageData(text: String(localized: "logout_success")))
    }

    private func showLogin(message: MessageData? = nil) {
        messageData = message
        screen = .login(LoginViewModel(mobileCoreApi: mobileCoreApi))
    }
}
