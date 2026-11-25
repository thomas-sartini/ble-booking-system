package ch.hslu.blebooking.mobile.ui.screens.scanning

import android.bluetooth.BluetoothAdapter
import android.content.*
import android.location.LocationManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.hslu.blebooking.mobile.ble.scanning.DiscoveryError
import ch.hslu.blebooking.mobile.R
import ch.hslu.blebooking.mobile.ble.common.DiscoveredTerminal
import ch.hslu.blebooking.mobile.ui.components.AppTopBar
import ch.hslu.blebooking.mobile.ui.components.SignalStrength

@Composable
fun ScanningView(onTerminalSelected: (DiscoveredTerminal) -> Unit, onBack: () -> Unit,
    viewModel: ScanningViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val errorMessage = when (val error = state.error) {
        DiscoveryError.Unavailable -> stringResource(R.string.ble_unavailable_hint)
        is DiscoveryError.ScanFailed -> stringResource(R.string.scanning_failed, error.code)
        null -> null
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.startScanning()
                Lifecycle.Event.ON_PAUSE -> viewModel.stopScanning()
                else -> Unit
            }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { viewModel.availabilityChanged() }
        }
        ContextCompat.registerReceiver(context, receiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED).apply { addAction(LocationManager.MODE_CHANGED_ACTION) },
            ContextCompat.RECEIVER_EXPORTED)
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) viewModel.startScanning()
        onDispose { lifecycle.removeObserver(observer); context.unregisterReceiver(receiver); viewModel.stopScanning() }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppTopBar(title = stringResource(R.string.scanning_title), onBack = onBack)
            if (state.isScanning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.scanning_loading))
                }
            }
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!state.isScanning && errorMessage == null && state.terminals.isEmpty()) {
                Text(stringResource(R.string.scanning_empty))
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)) {
                items(state.terminals, key = { it.address }) { terminal ->
                    Card(onClick = { viewModel.stopScanning(); onTerminalSelected(terminal) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(terminal.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.terminal_unnamed),
                                    style = MaterialTheme.typography.titleMedium)
                                Text(terminal.address, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            SignalStrength(terminal.rssi)
                        }
                    }
                }
            }
        }
    }
}
