import Foundation
import SharedLogic
import SwiftUI

@MainActor
struct HomeView: View {
    let viewModel: HomeViewModel
    var messageData: MessageData? = nil

    @State private var bookingsReload = 0

    private var bookingsViewModel: BookingsViewModel { viewModel.bookingsViewModel }

    private var displayName: String {
        guard let profile = viewModel.userProfile else { return "" }
        return "\(profile.firstName) \(profile.lastName)"
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    if let error = viewModel.errorMessage {
                        Message(text: error, type: .error)
                    } else if let messageData {
                        Message(text: messageData.text, type: messageData.type)
                    }

                    profileHeader

                    Text("home_title")
                        .font(.largeTitle.bold())
                        .accessibilityAddTraits(.isHeader)

                    BookingsFeedback(state: bookingsViewModel.state, onRetry: reloadBookings)
                        .disabled(viewModel.isLoading)

                    if let booking = bookingsViewModel.featuredBooking {
                        BookingCard(booking: booking)
                    }

                    NavigationLink {
                        BookingsView(viewModel: bookingsViewModel, onRefresh: reloadBookings)
                    } label: {
                        Text("bookings_see_all")
                            .underline()
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(BLETheme.accent)
                            .padding(.vertical, 8)
                    }
                    .frame(maxWidth: .infinity)
                    .disabled(viewModel.isLoading)

                    if viewModel.isLoading {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                    }
                }
                .foregroundStyle(BLETheme.text)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 24)
                .padding(.vertical, 16)
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                terminalControls
            }
            .background(BLETheme.background.ignoresSafeArea())
            .navigationTitle(Text("login_brand"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                AppToolbar(
                    enabled: !viewModel.isLoading && !viewModel.isLoadingProfile && !bookingsViewModel.isLoading,
                    onLogout: { viewModel.logout() }
                )
            }
            .task { await viewModel.loadUserProfile() }
        }
        .task(id: bookingsReload) { await bookingsViewModel.loadBookings() }
        .onDisappear { viewModel.cancelLogout() }
    }

    private func reloadBookings() {
        guard !bookingsViewModel.isLoading else { return }
        bookingsReload += 1
    }

    private var profileHeader: some View {
        HStack(alignment: .center, spacing: 12) {
            Image(systemName: "person.fill")
                .font(.body.weight(.semibold))
                .foregroundStyle(BLETheme.secondaryText)
                .frame(width: 44, height: 44)
                .background(BLETheme.surfaceVariant, in: Circle())
                .overlay { Circle().strokeBorder(BLETheme.outline.opacity(0.4)) }
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 2) {
                Text(displayName.isEmpty ? LocalizedStringKey("home_welcome") : LocalizedStringKey("home_welcome_back"))
                    .font(.caption)
                    .foregroundStyle(BLETheme.secondaryText)
                if !displayName.isEmpty {
                    Text(verbatim: displayName)
                        .font(.headline)
                }
            }
            .accessibilityElement(children: .combine)
            .frame(maxWidth: .infinity, alignment: .leading)

            if viewModel.isLoadingProfile {
                ProgressView()
            } else if viewModel.userProfile != nil {
                HStack(spacing: 6) {
                    Circle()
                        .fill(BLETheme.success.text)
                        .frame(width: 7, height: 7)
                        .accessibilityHidden(true)
                    Text("home_status_online")
                        .font(.caption.weight(.medium))
                }
                .foregroundStyle(BLETheme.success.text)
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(BLETheme.success.background, in: Capsule())
                .overlay { Capsule().strokeBorder(BLETheme.accent.opacity(0.35)) }
            }
        }
    }

    private var terminalControls: some View {
        VStack(spacing: 10) {
            Label("home_bluetooth_demo", systemImage: "antenna.radiowaves.left.and.right")
                .font(.caption.weight(.medium))
                .foregroundStyle(BLETheme.secondaryText)

            Button(action: {}) {
                Label("home_connect_terminal", systemImage: "antenna.radiowaves.left.and.right")
                    .font(.headline)
            }
            .buttonStyle(BLEPrimaryButtonStyle())
            .disabled(true)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 12)
        .background(BLETheme.background)
    }
}

#if DEBUG
#Preview("Home — Light") {
    HomeView(viewModel: HomeViewModel(mobileCoreApi: PreviewMobileCoreApi()))
        .tint(BLETheme.accent)
        .preferredColorScheme(.light)
}

#Preview("Home — Dark") {
    HomeView(viewModel: HomeViewModel(mobileCoreApi: PreviewMobileCoreApi()))
        .tint(BLETheme.accent)
        .preferredColorScheme(.dark)
}
#endif
