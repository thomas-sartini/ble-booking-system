import SharedLogic

@MainActor
final class AppContainer {
    private let mobileCoreApi: MobileCoreApi

    init(mobileCoreApi: MobileCoreApi) {
        self.mobileCoreApi = mobileCoreApi
    }

    func makeLoginViewModel() -> LoginViewModel {
        LoginViewModel(mobileCoreApi: mobileCoreApi)
    }

    func makeHomeViewModel() -> HomeViewModel {
        HomeViewModel(mobileCoreApi: mobileCoreApi)
    }
}
