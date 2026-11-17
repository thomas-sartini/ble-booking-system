package ch.hslu.blebooking.mobile.ui.screens.terminal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import ch.hslu.blebooking.mobile.R
import ch.hslu.blebooking.mobile.ble.model.DiscoveredTerminal
import ch.hslu.blebooking.mobile.ui.components.AppTopBar
import ch.hslu.blebooking.mobile.ui.theme.BleTheme

@Composable
fun TerminalView(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    isLoggingOut: Boolean = false,
    modifier: Modifier = Modifier,
    terminalViewModel: TerminalViewModel = hiltViewModel(),
) {
    DisposableEffect(terminalViewModel) {
        onDispose { terminalViewModel.disconnect() }
    }
    TerminalContent(
        terminal = terminalViewModel.terminal,
        onBack = onBack,
        modifier = modifier,
        onLogout = onLogout,
        isLoggingOut = isLoggingOut,
        isConnected = terminalViewModel.isConnected,
        isConnecting = terminalViewModel.isConnecting,
        isBusy = terminalViewModel.isBusy,
        terminalId = terminalViewModel.terminalId,
        status = terminalViewModel.status,
        errorMessage = terminalViewModel.errorMessage,
        onConnect = terminalViewModel::connect,
        onDisconnect = terminalViewModel::disconnect,
        onOpen = terminalViewModel::openDoor,
        onClose = terminalViewModel::closeDoor,
    )
}

@Composable
private fun TerminalContent(
    terminal: DiscoveredTerminal,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
    isLoggingOut: Boolean = false,
    isConnected: Boolean = false,
    isConnecting: Boolean = false,
    isBusy: Boolean = false,
    terminalId: String? = null,
    status: String = "Disconnected",
    errorMessage: String? = null,
    onConnect: () -> Unit = {},
    onDisconnect: () -> Unit = {},
    onOpen: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding().padding(24.dp)) {
            AppTopBar(title = stringResource(R.string.terminal_title), onBack = onBack,
                showMenu = true, enabled = !isLoggingOut, onLogout = onLogout)
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        TerminalDetail(stringResource(R.string.terminal_name),
                            terminal.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.terminal_unnamed))
                        TerminalDetail(stringResource(R.string.terminal_address), terminal.address)
                        TerminalDetail(stringResource(R.string.terminal_signal), "${terminal.rssi} dBm")
                        terminalId?.let { TerminalDetail("Terminal ID", it) }
                        TerminalDetail("Status", status)
                    }
                }
                if (isConnecting || isBusy) CircularProgressIndicator(Modifier.size(24.dp))
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.height(24.dp))
            if (isConnected) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = onOpen, enabled = !isBusy && !isLoggingOut, modifier = Modifier.weight(1f)) {
                        Text("Open")
                    }
                    Button(onClick = onClose, enabled = !isBusy && !isLoggingOut, modifier = Modifier.weight(1f)) {
                        Text("Close")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Button(
                onClick = if (isConnected || isConnecting) onDisconnect else onConnect,
                enabled = !isLoggingOut,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(when {
                    isConnected -> "Disconnect"
                    isConnecting -> "Cancel connection"
                    else -> stringResource(R.string.terminal_connect)
                })
            }
        }
    }
}

@Composable
private fun TerminalDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(name = "Terminal Light", showBackground = true)
@Composable
private fun TerminalLightPreview() {
    BleTheme(darkTheme = false) {
        TerminalContent(DiscoveredTerminal("Meeting room", "02:00:00:00:00:01", -55))
    }
}

@Preview(name = "Terminal Dark", showBackground = true)
@Composable
private fun TerminalDarkPreview() {
    BleTheme(darkTheme = true) {
        TerminalContent(DiscoveredTerminal(null, "02:00:00:00:00:03", -84))
    }
}
