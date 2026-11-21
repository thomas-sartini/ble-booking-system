package ch.hslu.blebooking.mobile.ui.screens.scanning

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import ch.hslu.blebooking.mobile.ble.api.BlePermissions
import ch.hslu.blebooking.mobile.ble.api.BleScanListener
import ch.hslu.blebooking.mobile.ble.api.BleScanner
import ch.hslu.blebooking.mobile.ble.model.DiscoveredTerminal
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class ScanningViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel(), BleScanListener {
    private val scanner = BleScanner(context)

    init {
        scanner.addScanListener(this)
    }

    var terminals by mutableStateOf<List<DiscoveredTerminal>>(emptyList())
        private set
    var isScanning by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** Starts once while the screen is visible; permission dialogs belong to the view. */
    fun startScanning() {
        if (isScanning) return
        if (!BlePermissions.hasPermissions(context)) {
            errorMessage = "Allow Bluetooth permissions to find nearby resources."
            return
        }
        errorMessage = null
        terminals = emptyList()
        try {
            isScanning = true
            scanner.startScan()
        } catch (_: SecurityException) {
            isScanning = false
            errorMessage = "Allow Bluetooth permissions to find nearby resources."
        } catch (_: IllegalStateException) {
            isScanning = false
            errorMessage = "Scanning is unavailable. Check that Bluetooth is enabled and try again."
        }
    }

    fun stopScanning() {
        isScanning = false
        try {
            scanner.stopScan()
        } catch (_: SecurityException) {
            // Permissions may have been revoked while scanning.
        }
    }

    override fun onScanFailed(errorCode: Int) {
        if (!isScanning) return
        stopScanning()
        errorMessage = "Bluetooth scan failed (code $errorCode). Check Bluetooth and try again."
    }

    /** Repeated advertisements refresh the same card, including its name and signal strength. */
    override fun onTerminalDiscovered(terminal: DiscoveredTerminal) {
        if (!isScanning) return
        val index = terminals.indexOfFirst { it.address == terminal.address }
        terminals = terminals.toMutableList().apply {
            if (index < 0) {
                add(terminal)
            } else {
                val previous = this[index]
                this[index] = terminal.copy(name = terminal.name?.takeIf { it.isNotBlank() } ?: previous.name)
            }
        }
    }

    override fun onCleared() {
        stopScanning()
        scanner.removeScanListener(this)
        super.onCleared()
    }
}
