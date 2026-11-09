import SwiftUI

@MainActor
struct HomeView: View {
    @Bindable var viewModel: HomeViewModel
    let messageData: MessageData?

    init(
        viewModel: HomeViewModel,
        messageData: MessageData? = nil
    ) {
        self.viewModel = viewModel
        self.messageData = messageData
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    if let errorMessage = viewModel.errorMessage {
                        Message(text: errorMessage, type: .error)
                    }

                    if viewModel.errorMessage == nil, let messageData {
                        Message(text: messageData.text, type: messageData.type)
                    }

                    if viewModel.isLoading {
                        HStack {
                            Spacer()
                            ProgressView()
                                .controlSize(.large)
                            Spacer()
                        }
                        .padding(.top, 32)
                    }

                    Spacer()
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 24)
            }
            .navigationTitle(Text("home_title"))
            .toolbar {
                AppToolbar(
                    showMenu: true,
                    enabled: !viewModel.isLoading,
                    onLogout: {
                        viewModel.logout()
                    }
                )
            }
            .onChange(of: viewModel.didLogout) { _, didLogout in
                if didLogout {
                    onLogoutSuccess()
                }
            }
            .onDisappear {
                viewModel.cancelLogout()
            }
        }
    }

    private func onLogoutSuccess() {
        // Integrata internamente come nella versione Android
    }
}

#Preview("Home") {
    let api = PreviewMobileCoreApi()
    let viewModel = HomeViewModel(mobileCoreApi: api)
    HomeView(
        viewModel: viewModel,
        messageData: MessageData(text: "Login successful", type: .success)
    )
}
