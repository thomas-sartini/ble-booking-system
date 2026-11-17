package ch.hslu.blebooking.mobile.ui.screens.scanning

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ch.hslu.blebooking.mobile.R
import ch.hslu.blebooking.mobile.ble.api.BlePermissions
import ch.hslu.blebooking.mobile.ble.model.DiscoveredTerminal
import ch.hslu.blebooking.mobile.ui.components.AppTopBar
import ch.hslu.blebooking.mobile.ui.theme.BleTheme

@Composable
fun ScanningView(
    modifier: Modifier = Modifier,
    onTerminalSelected: (DiscoveredTerminal) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    isLoggingOut: Boolean = false,
    scanningViewModel: ScanningViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            scanningViewModel.startScanning()
        }
    }

    LaunchedEffect(Unit) {
        if (!BlePermissions.hasPermissions(context) && !permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(BlePermissions.requiredPermissions())
        }
    }

    // Stop in the background or on navigation away; resume when this screen becomes active.
    DisposableEffect(lifecycleOwner, scanningViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> scanningViewModel.startScanning()
                Lifecycle.Event.ON_PAUSE -> scanningViewModel.stopScanning()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            scanningViewModel.startScanning()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            scanningViewModel.stopScanning()
        }
    }

    ScanningContent(
        modifier = modifier,
        terminals = scanningViewModel.terminals,
        isScanning = scanningViewModel.isScanning,
        errorMessage = scanningViewModel.errorMessage,
        onTerminalSelected = onTerminalSelected,
        onBack = onBack,
        onLogout = onLogout,
        isLoggingOut = isLoggingOut,
        onRetry = {
            if (BlePermissions.hasPermissions(context)) scanningViewModel.startScanning()
            else permissionLauncher.launch(BlePermissions.requiredPermissions())
        },
    )
}

@Composable
private fun ScanningContent(
    modifier: Modifier = Modifier,
    terminals: List<DiscoveredTerminal> = emptyList(),
    isScanning: Boolean = true,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    onTerminalSelected: (DiscoveredTerminal) -> Unit = {},
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    isLoggingOut: Boolean = false,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 24.dp, vertical = 24.dp)) {
            AppTopBar(title = stringResource(R.string.scanning_title), onBack = onBack,
                showMenu = true, enabled = !isLoggingOut, onLogout = onLogout)
            if (terminals.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                ) {
                    if (isScanning) CircularProgressIndicator(Modifier.size(48.dp))
                    Text(
                        text = errorMessage ?: stringResource(R.string.scanning_loading),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    if (errorMessage != null) Button(onClick = onRetry) { Text("Try again") }
                }
            } else {
                errorMessage?.let { message ->
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onRetry) { Text("Try again") }
                    }
                }
                if (isScanning) {
                    Row(
                        modifier = Modifier.padding(vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.scanning_loading), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(terminals, key = { it.address }) { terminal ->
                        TerminalCard(terminal, onClick = { onTerminalSelected(terminal) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalCard(terminal: DiscoveredTerminal, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = terminal.name?.takeIf { it.isNotBlank() } ?: "Unnamed resource",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            SignalStrength(terminal.rssi)
        }
    }
}

@Composable
private fun SignalStrength(rssi: Int) {
    // RSSI gives an approximate signal level, not a distance or an established connection.
    val bars = when {
        rssi >= -60 -> 4
        rssi >= -70 -> 3
        rssi >= -80 -> 2
        else -> 1
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.height(24.dp).semantics {
                contentDescription = "Signal strength: $bars of 4 bars"
            },
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            repeat(4) { index ->
                Box(
                    Modifier.width(5.dp).height((6 * (index + 1)).dp).background(
                        if (index < bars) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.extraSmall,
                    ),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("$rssi dBm", style = MaterialTheme.typography.labelSmall)
    }
}

@Preview(name = "Scanning Light", showBackground = true)
@Composable
private fun ScanningLightPreview() {
    BleTheme(darkTheme = false) { ScanningContent() }
}

@Preview(name = "Scanning Results Dark", showBackground = true)
@Composable
private fun ScanningDarkPreview() {
    BleTheme(darkTheme = true) {
        ScanningContent(terminals = listOf(
            DiscoveredTerminal("Meeting room", "00:11:22:33:44:55", -55),
            DiscoveredTerminal("Office", "00:11:22:33:44:66", -76),
        ))
    }
}
