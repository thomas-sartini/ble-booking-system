import SwiftUI

@MainActor
struct ContentView: View {
    @State private var viewModel: AppViewModel
    @State private var restoreAttempt = 0

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

            case .restoreFailed:
                VStack(spacing: 16) {
                    Text("session_restore_error")
                        .multilineTextAlignment(.center)
                        .foregroundStyle(BLETheme.text)
                    Button("session_restore_retry") {
                        restoreAttempt += 1
                    }
                    .buttonStyle(BLEPrimaryButtonStyle())
                }
                .padding(24)
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
        .task(id: restoreAttempt) { await viewModel.restoreSession() }
    }
}
