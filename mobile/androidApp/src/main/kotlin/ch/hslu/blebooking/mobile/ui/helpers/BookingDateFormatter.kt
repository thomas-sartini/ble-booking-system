package ch.hslu.blebooking.mobile.ui.helpers

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Instant

/** Formats booking times in the device zone, always using a 24-hour clock. */
object BookingDateFormatter {
    fun day(instant: Instant, locale: Locale, zone: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale).withZone(zone)
            .format(java.time.Instant.parse(instant.toString()))

    fun timeRange(start: Instant, end: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withZone(zone)
        return formatter.format(java.time.Instant.parse(start.toString())) + " – " +
            formatter.format(java.time.Instant.parse(end.toString()))
    }
}
