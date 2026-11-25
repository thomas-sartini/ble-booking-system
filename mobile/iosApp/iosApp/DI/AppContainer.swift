import SharedLogic

@MainActor
final class AppContainer {
    private let mobileCoreApi: MobileCoreApi
    private let secureStorage: NativeStorageDelegate

    init(mobileCoreApi: MobileCoreApi, secureStorage: NativeStorageDelegate) {
        self.mobileCoreApi = mobileCoreApi
        self.secureStorage = secureStorage
    }

    func makeAppViewModel() -> AppViewModel {
        AppViewModel(mobileCoreApi: mobileCoreApi, secureStorage: secureStorage)
    }
}
