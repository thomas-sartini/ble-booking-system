import Foundation
import SharedLogic
import SwiftUI

@MainActor
struct HomeView: View {
    let viewModel: HomeViewModel
    var messageData: MessageData? = nil

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

                    VStack(alignment: .leading, spacing: 4) {
                        Text("home_title")
                            .font(.largeTitle.bold())
                            .accessibilityAddTraits(.isHeader)
                        Text("home_subtitle")
                            .font(.subheadline)
                            .foregroundStyle(BLETheme.secondaryText)
                    }

                    DemoBookingCard()

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
                    enabled: !viewModel.isLoading && !viewModel.isLoadingProfile,
                    onLogout: { viewModel.logout() }
                )
            }
            .task { await viewModel.loadUserProfile() }
            .onDisappear { viewModel.cancelLogout() }
        }
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

private struct DemoBookingCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            ViewThatFits(in: .horizontal) {
                HStack {
                    Text("home_active_booking_label")
                        .fixedSize(horizontal: true, vertical: false)
                    Spacer()
                    Text("home_active_booking_date")
                        .fixedSize(horizontal: true, vertical: false)
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text("home_active_booking_label")
                    Text("home_active_booking_date")
                }
            }
            .font(.caption.weight(.semibold))
            .foregroundStyle(BLETheme.secondaryText)

            HStack(spacing: 14) {
                Image(systemName: "door.left.hand.closed")
                    .font(.title3)
                    .foregroundStyle(BLETheme.secondaryText)
                    .frame(width: 46, height: 46)
                    .background(BLETheme.surfaceVariant, in: RoundedRectangle(cornerRadius: 12))
                    .accessibilityHidden(true)
                Text("home_demo_booking_room")
                    .font(.title2.bold())
            }

            Divider()

            detail("home_booking_time_slot_label", value: "home_demo_booking_time", icon: "clock")
            detail("home_booking_location_label", value: "home_demo_booking_location", icon: "mappin")

            Label("home_booking_status_ready", systemImage: "lock.open")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(BLETheme.accent)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(BLETheme.surface, in: RoundedRectangle(cornerRadius: 16))
        .overlay {
            RoundedRectangle(cornerRadius: 16)
                .strokeBorder(BLETheme.outline.opacity(0.25))
        }
        .accessibilityElement(children: .contain)
    }

    private func detail(_ title: LocalizedStringKey, value: LocalizedStringKey, icon: String) -> some View {
        Label {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.caption)
                    .foregroundStyle(BLETheme.secondaryText)
                Text(value)
                    .font(.subheadline.weight(.medium))
            }
        } icon: {
            Image(systemName: icon)
                .foregroundStyle(BLETheme.secondaryText)
        }
        .accessibilityElement(children: .combine)
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
