import Foundation
import SharedLogic

/// Uses the device time zone and a 24-hour clock, matching the Android booking cards.
enum BookingDateFormatter {
    static func day(
        _ instant: KotlinInstant,
        locale: Locale = .current,
        timeZone: TimeZone = .current
    ) -> String {
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.timeZone = timeZone
        formatter.dateStyle = .medium
        return formatter.string(from: date(from: instant))
    }

    static func timeRange(
        start: KotlinInstant,
        end: KotlinInstant,
        locale: Locale = .current,
        timeZone: TimeZone = .current
    ) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = timeZone
        formatter.dateFormat = "HH:mm"

        let startDate = date(from: start)
        let endDate = date(from: end)
        let startTime = formatter.string(from: startDate)
        let endTime = formatter.string(from: endDate)

        var calendar = Calendar.current
        calendar.timeZone = timeZone
        if calendar.isDate(startDate, inSameDayAs: endDate) {
            return "\(startTime) – \(endTime)"
        }
        return "\(startTime) – \(day(end, locale: locale, timeZone: timeZone)), \(endTime)"
    }

    private static func date(from instant: KotlinInstant) -> Date {
        Date(timeIntervalSince1970: Double(instant.epochSeconds)
            + Double(instant.nanosecondsOfSecond) / 1_000_000_000)
    }
}
