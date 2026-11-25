import SwiftUI

@MainActor
struct BookingsView: View {
    let viewModel: BookingsViewModel
    let onRefresh: () -> Void

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 14) {
                BookingsFeedback(state: viewModel.state, onRetry: onRefresh)

                if case .ready(let bookings) = viewModel.state {
                    ForEach(bookings, id: \.id) { booking in
                        BookingCard(booking: booking)
                    }
                }
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
        }
        .background(BLETheme.background.ignoresSafeArea())
        .navigationTitle(Text("bookings_title"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(action: onRefresh) {
                    Label("bookings_refresh", systemImage: "arrow.clockwise")
                }
                .disabled(viewModel.isLoading)
            }
        }
        .refreshable { await viewModel.loadBookings() }
    }
}
