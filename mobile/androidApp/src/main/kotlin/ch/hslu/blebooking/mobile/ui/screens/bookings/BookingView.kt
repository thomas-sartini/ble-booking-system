package ch.hslu.blebooking.mobile.ui.screens.bookings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.hslu.blebooking.mobile.R
import ch.hslu.blebooking.mobile.ui.components.AppTopBar
import ch.hslu.blebooking.mobile.ui.components.BookingCard
import ch.hslu.blebooking.mobile.ui.components.BookingsFeedback

@Composable
fun BookingView(onBack: () -> Unit, viewModel: BookingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            AppTopBar(
                title = stringResource(R.string.bookings_title),
                onBack = onBack,
                onRefresh = viewModel::loadBookings,
                refreshEnabled = state !is BookingsState.Loading,
            )
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { BookingsFeedback(state) }
                val ready = state as? BookingsState.Ready
                items(ready?.bookings.orEmpty(), key = { it.id }) { BookingCard(it) }
            }
        }
    }
}
