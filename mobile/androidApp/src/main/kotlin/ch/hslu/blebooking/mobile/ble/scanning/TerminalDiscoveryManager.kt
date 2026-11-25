package ch.hslu.blebooking.mobile.ble.scanning

import ch.hslu.blebooking.mobile.ble.common.DiscoveredTerminal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DiscoveryError {
    data object Unavailable : DiscoveryError
    data class ScanFailed(val code: Int) : DiscoveryError
}

data class TerminalDiscoveryState(
    val terminals: List<DiscoveredTerminal> = emptyList(),
    val isScanning: Boolean = false,
    val error: DiscoveryError? = null,
    val selectedTerminal: DiscoveredTerminal? = null,
    val isRefreshingSignal: Boolean = false,
    val signalNotDetected: Boolean = false,
    val signalRefreshFailed: Boolean = false,
)

/** Owns observations and scan lifetimes. Call on the main thread, like the scanner callbacks. */
class TerminalDiscoveryManager(
    private val scanner: TerminalScanner,
    private val isAvailable: () -> Boolean,
    private val scope: CoroutineScope,
    private val elapsedMillis: () -> Long,
) : BleScanListener {
    private data class Observation(val terminal: DiscoveredTerminal, val lastSeen: Long)
    private val observations = mutableMapOf<String, Observation>()
    private val _state = MutableStateFlow(TerminalDiscoveryState())
    val state = _state.asStateFlow()
    private var monitor: Job? = null
    private var closed = false

    init { scanner.addScanListener(this) }

    /** Starts continuous discovery until the screen stops it or Bluetooth becomes unavailable. */
    fun startDiscovery() {
        if (closed || _state.value.isScanning) return
        stopDiscovery()
        observations.clear()
        _state.value = _state.value.copy(terminals = emptyList(), error = null)
        if (!isAvailable()) { fail(DiscoveryError.Unavailable); return }
        _state.value = _state.value.copy(isScanning = true)
        try {
            scanner.startScan()
            monitor = scope.launch {
                while (_state.value.isScanning) {
                    delay(1_000)
                    if (!isAvailable()) { availabilityChanged(); break }
                    val now = elapsedMillis()
                    // No explicit "device disappeared" event exists: expire stale advertisements.
                    observations.entries.removeAll { now - it.value.lastSeen >= LOST_AFTER_MS }
                    publishObservations()
                }
            }
        } catch (_: RuntimeException) { fail(DiscoveryError.Unavailable) }
    }

    fun stopDiscovery() {
        // Stop both the radio and the expiry/refresh timer when leaving the screen.
        monitor?.cancel()
        monitor = null
        _state.value = _state.value.copy(isScanning = false, isRefreshingSignal = false)
        scanner.stopScan()
    }

    fun selectTerminal(terminal: DiscoveredTerminal) {
        stopDiscovery()
        _state.value = _state.value.copy(selectedTerminal = terminal,
            signalNotDetected = false, signalRefreshFailed = false)
    }

    /** Briefly scans for the selected address without replacing its name or other details. */
    fun refreshSignal() {
        if (closed || _state.value.isRefreshingSignal || _state.value.selectedTerminal == null) return
        stopDiscovery()
        _state.value = _state.value.copy(error = null, signalRefreshFailed = false)
        if (!isAvailable()) {
            _state.value = _state.value.copy(signalRefreshFailed = true)
            return
        }
        _state.value = _state.value.copy(isRefreshingSignal = true)
        try {
            scanner.startScan()
            monitor = scope.launch {
                repeat(5) {
                    delay(1_000)
                    if (!isAvailable()) { availabilityChanged(); return@launch }
                }
                // A successful scan with no matching advertisement is not a scan failure.
                _state.value = _state.value.copy(signalNotDetected = true)
                stopDiscovery()
            }
        } catch (_: RuntimeException) { fail(DiscoveryError.Unavailable) }
    }

    fun availabilityChanged() {
        if (!isAvailable()) {
            observations.clear()
            _state.value = _state.value.copy(terminals = emptyList())
            fail(DiscoveryError.Unavailable)
        }
    }

    override fun onTerminalDiscovered(terminal: DiscoveredTerminal) {
        val current = _state.value
        if (current.isRefreshingSignal) {
            // Other nearby devices must not update the selected terminal's signal.
            if (terminal.address == current.selectedTerminal?.address) {
                _state.value = current.copy(selectedTerminal = current.selectedTerminal.copy(rssi = terminal.rssi),
                    signalNotDetected = false, signalRefreshFailed = false)
                stopDiscovery()
            }
        } else if (current.isScanning) {
            val previous = observations[terminal.address]?.terminal
            // Repeated advertisements update one entry; some packets omit the device name.
            val updated = terminal.copy(name = terminal.name?.takeIf { it.isNotBlank() } ?: previous?.name)
            observations[terminal.address] = Observation(updated, elapsedMillis())
            publishObservations()
        }
    }

    override fun onScanFailed(errorCode: Int) { fail(DiscoveryError.ScanFailed(errorCode)) }

    private fun fail(error: DiscoveryError) {
        _state.value = _state.value.copy(error = error,
            signalRefreshFailed = _state.value.isRefreshingSignal || _state.value.signalRefreshFailed)
        stopDiscovery()
    }

    private fun publishObservations() {
        _state.value = _state.value.copy(terminals = observations.values.map { it.terminal }
            .sortedWith(compareByDescending<DiscoveredTerminal> { it.rssi }.thenBy { it.address }))
    }

    fun close() {
        closed = true
        stopDiscovery()
        scanner.removeScanListener(this)
        observations.clear()
    }

    private companion object { const val LOST_AFTER_MS = 6_000L }
}
