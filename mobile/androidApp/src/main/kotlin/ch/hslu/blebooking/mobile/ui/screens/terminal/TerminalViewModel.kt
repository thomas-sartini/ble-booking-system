package ch.hslu.blebooking.mobile.ui.screens.terminal

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.hslu.blebooking.mobile.ble.common.*
import ch.hslu.blebooking.mobile.ble.scanning.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Owns the selected terminal and its signal checks, independently of the discovery screen. */
@HiltViewModel
class TerminalViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val discovery = TerminalDiscoveryManager(
        scanner = BleScanner(context),
        isAvailable = { bluetoothAvailability(context) == BleAvailability.READY },
        scope = viewModelScope,
        elapsedMillis = SystemClock::elapsedRealtime,
    )
    val state = discovery.state

    init {
        val address = savedStateHandle.get<String>("address")
        val rssi = savedStateHandle.get<Int>("rssi")
        if (!address.isNullOrBlank() && rssi != null) {
            discovery.selectTerminal(DiscoveredTerminal(savedStateHandle["name"], address, rssi))
        }
    }

    fun refreshSignal() = discovery.refreshSignal()
    fun stopRefresh() = discovery.stopDiscovery()
    override fun onCleared() = discovery.close()
}
