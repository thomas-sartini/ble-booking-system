package ch.hslu.blebooking.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.hslu.blebooking.mobile.R
import ch.hslu.blebooking.mobile.model.Booking
import ch.hslu.blebooking.mobile.model.BookingStatus
import androidx.compose.ui.platform.LocalConfiguration
import ch.hslu.blebooking.mobile.ui.helpers.BookingDateFormatter

@Composable
fun BookingCard(booking: Booking, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val status = when (booking.status) {
        BookingStatus.BOOKED -> R.string.booking_status_booked
        BookingStatus.CHECKED_IN -> R.string.booking_status_checked_in
        BookingStatus.COMPLETED -> R.string.booking_status_completed
        BookingStatus.CANCELLED -> R.string.booking_status_cancelled
        BookingStatus.NO_SHOW -> R.string.booking_status_no_show
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.home_active_booking_label),
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text(booking.resourceName?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.booking_resource, booking.resourceId),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Surface(color = colors.primaryContainer, contentColor = colors.onPrimaryContainer,
                shape = RoundedCornerShape(50)) {
                Text(stringResource(status), Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider(color = colors.outlineVariant)
            Text(BookingDateFormatter.day(booking.startTime, locale),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(stringResource(R.string.home_booking_time_slot_label),
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text(BookingDateFormatter.timeRange(booking.startTime, booking.endTime),
                style = MaterialTheme.typography.bodyMedium)
            booking.resourcePosition?.takeIf { it.isNotBlank() }?.let { position ->
                Text(stringResource(R.string.home_booking_location_label),
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text(position, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Booking Light", showBackground = true)
@androidx.compose.ui.tooling.preview.Preview(name = "Booking Dark", showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookingCardPreview() {
    ch.hslu.blebooking.mobile.ui.theme.BleTheme {
        BookingCard(
            Booking("1", "42", kotlin.time.Instant.parse("2026-11-25T09:00:00Z"),
                kotlin.time.Instant.parse("2026-11-25T10:00:00Z"), BookingStatus.CHECKED_IN,
                "Meeting Room 101", "HQ · Floor 1 · East Wing"),
            Modifier.padding(16.dp),
        )
    }
}
