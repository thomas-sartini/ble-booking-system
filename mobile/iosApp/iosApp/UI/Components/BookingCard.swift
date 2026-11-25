import SharedLogic
import SwiftUI

struct BookingCard: View {
    let booking: Booking

    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone

    private var resourceName: String {
        if let name = booking.resourceName?.trimmingCharacters(in: .whitespacesAndNewlines), !name.isEmpty {
            return name
        }
        return String.localizedStringWithFormat(String(localized: "booking_resource"), booking.resourceId)
    }

    private var status: (title: LocalizedStringKey, color: Color, background: Color) {
        switch booking.status {
        case .booked:
            ("booking_status_booked", BLETheme.accent, BLETheme.surfaceVariant)
        case .checkedIn:
            ("booking_status_checked_in", BLETheme.success.text, BLETheme.success.background)
        case .completed:
            ("booking_status_completed", BLETheme.secondaryText, BLETheme.surfaceVariant)
        case .cancelled:
            ("booking_status_cancelled", BLETheme.secondaryText, BLETheme.surfaceVariant)
        case .noShow:
            ("booking_status_no_show", BLETheme.warning.text, BLETheme.warning.background)
        default:
            ("booking_status_unknown", BLETheme.secondaryText, BLETheme.surfaceVariant)
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top, spacing: 14) {
                Image(systemName: "calendar")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(BLETheme.accent)
                    .frame(width: 44, height: 44)
                    .background(BLETheme.surfaceVariant, in: RoundedRectangle(cornerRadius: 13))
                    .accessibilityHidden(true)

                VStack(alignment: .leading, spacing: 8) {
                    Text(verbatim: resourceName)
                        .font(.title3.bold())
                        .accessibilityAddTraits(.isHeader)

                    Text(status.title)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(status.color)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(status.background, in: Capsule())
                }
            }

            Divider()

            VStack(alignment: .leading, spacing: 10) {
                detail(
                    BookingDateFormatter.day(booking.startTime, locale: locale, timeZone: timeZone),
                    icon: "calendar"
                )
                detail(
                    BookingDateFormatter.timeRange(
                        start: booking.startTime, end: booking.endTime, locale: locale, timeZone: timeZone
                    ),
                    icon: "clock"
                )
                if let position = booking.resourcePosition?.trimmingCharacters(in: .whitespacesAndNewlines),
                   !position.isEmpty {
                    detail(position, icon: "mappin.circle")
                }
            }
        }
        .foregroundStyle(BLETheme.text)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(BLETheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .overlay {
            RoundedRectangle(cornerRadius: 20)
                .strokeBorder(BLETheme.outline.opacity(0.24))
        }
        .accessibilityElement(children: .contain)
    }

    private func detail(_ value: String, icon: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 10) {
            Image(systemName: icon)
                .foregroundStyle(BLETheme.accent)
                .frame(width: 19)
                .accessibilityHidden(true)
            Text(verbatim: value)
                .foregroundStyle(BLETheme.secondaryText)
        }
        .font(.subheadline)
    }
}
