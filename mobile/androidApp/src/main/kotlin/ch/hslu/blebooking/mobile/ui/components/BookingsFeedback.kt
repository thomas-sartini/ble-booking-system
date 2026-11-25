package ch.hslu.blebooking.mobile.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.hslu.blebooking.mobile.R
import ch.hslu.blebooking.mobile.ui.screens.bookings.BookingsState

@Composable
fun BookingsFeedback(state: BookingsState, onRetry: (() -> Unit)? = null) {
    when (state) {
        BookingsState.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is BookingsState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Message(stringResource(if (state.sessionExpired) R.string.bookings_session_expired
                else R.string.bookings_error), MessageType.Error)
            if (onRetry != null) {
                TextButton(onClick = onRetry) { Text(stringResource(R.string.session_restore_retry)) }
            }
        }
        is BookingsState.Ready -> if (state.bookings.isEmpty()) {
            Text(stringResource(R.string.bookings_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
