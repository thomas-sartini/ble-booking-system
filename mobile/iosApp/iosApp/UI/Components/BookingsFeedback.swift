import SwiftUI

struct BookingsFeedback: View {
    let state: BookingsViewModel.State
    let onRetry: () -> Void

    var body: some View {
        switch state {
        case .loading:
            ProgressView()
                .accessibilityLabel(Text("bookings_loading"))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 24)
        case .ready(let bookings):
            if bookings.isEmpty {
                ContentUnavailableView {
                    Label("bookings_empty_title", systemImage: "calendar")
                } description: {
                    Text("bookings_empty")
                }
                .frame(maxWidth: .infinity)
                .background(BLETheme.surface, in: RoundedRectangle(cornerRadius: 20))
            }
        case .failed(let sessionExpired):
            VStack(alignment: .leading, spacing: 8) {
                Message(
                    text: sessionExpired
                        ? String(localized: "bookings_session_expired")
                        : String(localized: "bookings_error"),
                    type: .error
                )
                Button("session_restore_retry", action: onRetry)
                    .font(.subheadline.weight(.semibold))
                    .padding(.vertical, 6)
            }
        }
    }
}
