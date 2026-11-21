package ch.hslu.blebooking.mobile.ui.screens.terminal

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.hslu.blebooking.mobile.ble.api.BleConnectionListener
import ch.hslu.blebooking.mobile.ble.api.BlePermissions
import ch.hslu.blebooking.mobile.ble.api.TerminalManager
import ch.hslu.blebooking.mobile.ble.gatt.BleGattSession
import ch.hslu.blebooking.mobile.ble.api.BleConnectionManager
import ch.hslu.blebooking.mobile.ui.navigation.TerminalRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
) : ViewModel(), BleConnectionListener {
    val terminal = TerminalRoute.read(savedStateHandle)
    private val connection = BleConnectionManager(context)

    init {
        connection.addConnectionListener(this)
    }
    private var manager: TerminalManager? = null
    private var timeout: Job? = null
    private var operation: Job? = null

    var isConnected by mutableStateOf(false)
        private set
    var isConnecting by mutableStateOf(false)
        private set
    var isBusy by mutableStateOf(false)
        private set
    var terminalId by mutableStateOf<String?>(null)
        private set
    var status by mutableStateOf("Disconnected")
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun connect() {
        if (isConnected || isConnecting) return
        errorMessage = null
        if (!BlePermissions.hasPermissions(context)) {
            errorMessage = "Bluetooth permissions are required. Return to scanning to grant them."
            return
        }
        isConnecting = true
        status = "Connecting..."
        // Cover connection, service discovery and ID reading with one deadline.
        timeout = viewModelScope.launch {
            delay(CONNECTION_TIMEOUT_MS)
            timeout = null
            disconnect()
            errorMessage = "Connection timed out. Check that the terminal is nearby and switched on, then try again."
        }
        try {
            connection.connect(terminal.address)
        } catch (error: Exception) {
            disconnect()
            errorMessage = error.message ?: "Connection failed."
        }
    }

    override fun onReady(session: BleGattSession) {
        if (!isConnecting) return
        val readyManager = TerminalManager(session)
        manager = readyManager
        operation = viewModelScope.launch {
            try {
                terminalId = readyManager.readTerminalId().toString()
                timeout?.cancel()
                timeout = null
                isConnecting = false
                isConnected = true
                status = "Connected"
            } catch (_: TimeoutCancellationException) {
                disconnect()
                errorMessage = "The terminal did not respond in time. Please reconnect."
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                disconnect()
                errorMessage = error.message ?: "Unable to read terminal information."
            }
        }
    }

    fun openDoor() = executeCommand(open = true)
    fun closeDoor() = executeCommand(open = false)

    private fun executeCommand(open: Boolean) {
        val connectedManager = manager ?: return
        if (!isConnected || isBusy) return
        isBusy = true
        errorMessage = null
        status = if (open) "Releasing latch..." else "Locking..."
        operation = viewModelScope.launch {
            try {
                if (open) connectedManager.openDoor() else connectedManager.closeDoor()
                status = if (open) "Unlocked" else "Locked"
            } catch (_: TimeoutCancellationException) {
                disconnect()
                errorMessage = "Command timed out. The result is unknown; check the terminal before trying again."
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                errorMessage = error.message ?: "Command failed; outcome may be unknown."
            } finally {
                isBusy = false
            }
        }
    }

    fun disconnect() {
        timeout?.cancel()
        timeout = null
        operation?.cancel()
        operation = null
        try {
            connection.close()
        } finally {
            resetConnection()
        }
    }

    override fun onDisconnected() {
        timeout?.cancel()
        resetConnection()
    }

    override fun onConnectionError(message: String) {
        disconnect()
        errorMessage = message
    }

    private fun resetConnection() {
        manager = null
        isConnected = false
        isConnecting = false
        isBusy = false
        terminalId = null
        status = "Disconnected"
    }

    override fun onCleared() {
        disconnect()
        connection.removeConnectionListener(this)
        super.onCleared()
    }

    private companion object {
        const val CONNECTION_TIMEOUT_MS = 10_000L
    }
}
