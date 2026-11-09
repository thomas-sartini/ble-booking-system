import SwiftUI
import SharedLogic

@main
@MainActor
struct iOSApp: App {
    private let container: AppContainer

    init() {
        let storage = NativeSecureStorageAdapter(
            delegate: IosSecureStorage.shared
        )

        let api = MobileCoreFactory.shared.create(
            secureStorage: storage,
            httpClientEngine: nil
        )

        container = AppContainer(mobileCoreApi: api)

    }
    var body: some Scene {
        WindowGroup {
            ContentView(container: container)
        }
    }
}
