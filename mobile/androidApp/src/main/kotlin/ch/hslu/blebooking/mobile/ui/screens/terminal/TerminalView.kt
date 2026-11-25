package ch.hslu.blebooking.mobile.ui.screens.terminal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.hslu.blebooking.mobile.R
import ch.hslu.blebooking.mobile.ble.common.*
import ch.hslu.blebooking.mobile.ui.components.AppTopBar
import ch.hslu.blebooking.mobile.ui.components.SignalStrength
import ch.hslu.blebooking.mobile.ui.theme.BleTheme

@Composable
fun TerminalView(onBack: () -> Unit, viewModel: TerminalViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.stopRefresh()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); viewModel.stopRefresh() }
    }
    TerminalContent(
        terminal = state.selectedTerminal,
        onBack = onBack,
        onRefreshSignal = viewModel::refreshSignal,
        isRefreshingSignal = state.isRefreshingSignal,
        signalNotDetected = state.signalNotDetected,
        signalRefreshFailed = state.signalRefreshFailed,
    )
}

@Composable
private fun TerminalContent(
    terminal: DiscoveredTerminal?,
    onBack: () -> Unit = {},
    onRefreshSignal: () -> Unit = {},
    isRefreshingSignal: Boolean = false,
    signalNotDetected: Boolean = false,
    signalRefreshFailed: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    var detailsExpanded by rememberSaveable(terminal?.address) { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            AppTopBar(title = stringResource(R.string.terminal_title), onBack = onBack,
                onRefresh = if (terminal != null) onRefreshSignal else null,
                refreshEnabled = !isRefreshingSignal)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                if (terminal == null) {
                    Text(stringResource(R.string.terminal_missing))
                } else {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = colors.primaryContainer, shape = RoundedCornerShape(24.dp)) {
                            Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                                Icon(painterResource(R.drawable.ic_bluetooth), contentDescription = null,
                                    tint = colors.onPrimaryContainer, modifier = Modifier.size(36.dp))
                            }
                        }
                        Text(terminal.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.terminal_unnamed),
                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Surface(color = colors.primaryContainer, contentColor = colors.onPrimaryContainer,
                            shape = RoundedCornerShape(50)) {
                            Text(stringResource(R.string.terminal_detected),
                                Modifier.padding(horizontal = 14.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, colors.outlineVariant)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(stringResource(R.string.terminal_signal), style = MaterialTheme.typography.labelMedium,
                                        color = colors.onSurfaceVariant)
                                    Text(if (signalNotDetected) stringResource(R.string.terminal_signal_not_detected)
                                        else stringResource(R.string.terminal_signal_value, terminal.rssi),
                                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                }
                                if (isRefreshingSignal) {
                                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else if (!signalNotDetected) {
                                    SignalStrength(terminal.rssi)
                                }
                            }
                            if (signalRefreshFailed) {
                                Text(stringResource(R.string.terminal_signal_refresh_failed),
                                    style = MaterialTheme.typography.bodySmall, color = colors.error)
                            } else if (signalNotDetected) {
                                Text(stringResource(R.string.terminal_signal_not_detected_hint),
                                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                            HorizontalDivider(color = colors.outlineVariant)
                            TerminalDetail(stringResource(R.string.terminal_connection), stringResource(R.string.terminal_not_connected))
                        }
                    }
                    Text(stringResource(R.string.terminal_discovery_note), style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant)
                    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        TextButton(onClick = { detailsExpanded = !detailsExpanded }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(if (detailsExpanded) R.string.terminal_hide_details else R.string.terminal_show_details))
                        }
                        if (detailsExpanded) {
                            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                TerminalDetail(stringResource(R.string.terminal_address), terminal.address)
                                TerminalDetail(stringResource(R.string.terminal_service), BleProtocolConstants.SERVICE_UUID)
                            }
                        }
                    }
                }
            }
            if (terminal != null) {
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(stringResource(R.string.terminal_connect))
                }
            }
        }
    }
}

@Composable
private fun TerminalDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(name = "Terminal Light", showBackground = true)
@Preview(name = "Terminal Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TerminalPreview() {
    BleTheme { TerminalContent(DiscoveredTerminal("Meeting room", "02:00:00:00:00:01", -55), {}) }
}
