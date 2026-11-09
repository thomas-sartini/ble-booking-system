import SwiftUI

@MainActor
struct ContentView: View {
    private let container: AppContainer
    @State private var currentScreen: AppScreen = .login
    @State private var loginViewModel: LoginViewModel
    @State private var homeViewModel: HomeViewModel
    @State private var messageData: MessageData?

    init(container: AppContainer) {
        self.container = container
        _loginViewModel = State(initialValue: container.makeLoginViewModel())
        _homeViewModel = State(initialValue: container.makeHomeViewModel())
    }

    var body: some View {
        switch currentScreen {
        case .login:
            LoginView(
                viewModel: loginViewModel,
                messageData: messageData
            )
            .onChange(of: loginViewModel.didSignIn) { _, didSignIn in
                if didSignIn {
                    messageData = MessageData(
                        text: String(localized: "home_login_success")
                    )
                    homeViewModel = container.makeHomeViewModel()
                    currentScreen = .home
                }
            }

        case .home:
            HomeView(
                viewModel: homeViewModel,
                messageData: messageData
            )
            .onChange(of: homeViewModel.didLogout) { _, didLogout in
                if didLogout {
                    messageData = MessageData(
                        text: String(localized: "logout_success")
                    )
                    loginViewModel = container.makeLoginViewModel()
                    currentScreen = .login
                }
            }
        }
    }
}

private enum AppScreen {
    case login
    case home
}

