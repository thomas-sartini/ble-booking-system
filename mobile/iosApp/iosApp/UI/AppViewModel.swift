import Foundation
import Observation
import SharedLogic

@MainActor
@Observable
final class AppViewModel {
    enum Screen {
        case loading
        case restoreFailed
        case login(LoginViewModel)
        case home(HomeViewModel)
    }

    private let mobileCoreApi: MobileCoreApi
    @ObservationIgnored private var isRestoring = false

    private(set) var screen: Screen = .loading
    private(set) var messageData: MessageData?

    init(mobileCoreApi: MobileCoreApi) {
        self.mobileCoreApi = mobileCoreApi
    }

    func restoreSession() async {
        guard !isRestoring, !Task.isCancelled else { return }
        switch screen {
        case .loading, .restoreFailed:
            break
        case .login, .home:
            return
        }

        isRestoring = true
        screen = .loading
        defer { isRestoring = false }

        do {
            let result = try await mobileCoreApi.restoreSession()
            guard !Task.isCancelled else { return }
            switch result {
            case let authenticated as SessionRestoreResultAuthenticated:
                screen = .home(HomeViewModel(
                    mobileCoreApi: mobileCoreApi,
                    userProfile: authenticated.profile
                ))
            case is SessionRestoreResultUnauthenticated:
                showLogin()
            case is SessionRestoreResultFailed:
                screen = .restoreFailed
            default:
                screen = .restoreFailed
            }
        } catch {
            guard !Task.isCancelled else { return }
            screen = .restoreFailed
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
