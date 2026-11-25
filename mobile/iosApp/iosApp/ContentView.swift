import SwiftUI

@MainActor
struct ContentView: View {
    @State private var viewModel: AppViewModel

    init(container: AppContainer) {
        _viewModel = State(initialValue: container.makeAppViewModel())
    }

    var body: some View {
        Group {
            switch viewModel.screen {
            case .loading:
                ProgressView()
                    .controlSize(.large)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)

            case .login(let loginViewModel):
                LoginView(viewModel: loginViewModel, messageData: viewModel.messageData)
                    .onChange(of: loginViewModel.didSignIn) { _, didSignIn in
                        if didSignIn { viewModel.didSignIn() }
                    }

            case .home(let homeViewModel):
                HomeView(viewModel: homeViewModel, messageData: viewModel.messageData)
                    .onChange(of: homeViewModel.didLogout) { _, didLogout in
                        if didLogout { viewModel.didLogout() }
                    }
            }
        }
        .background(BLETheme.background.ignoresSafeArea())
        .tint(BLETheme.accent)
        .task { await viewModel.restoreSession() }
    }
}
