import SharedLogic

@MainActor
final class AppContainer {
    private let mobileCoreApi: MobileCoreApi

    init(mobileCoreApi: MobileCoreApi) {
        self.mobileCoreApi = mobileCoreApi
    }

    func makeAppViewModel() -> AppViewModel {
        AppViewModel(mobileCoreApi: mobileCoreApi)
    }
}
