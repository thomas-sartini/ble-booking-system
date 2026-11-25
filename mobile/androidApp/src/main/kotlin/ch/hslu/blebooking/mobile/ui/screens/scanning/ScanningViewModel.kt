package ch.hslu.blebooking.mobile.ui.screens.scanning

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.hslu.blebooking.mobile.ble.common.*
import ch.hslu.blebooking.mobile.ble.scanning.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class ScanningViewModel @Inject constructor(@ApplicationContext context: Context) : ViewModel() {
    private val discovery = TerminalDiscoveryManager(
        scanner = BleScanner(context),
        isAvailable = { bluetoothAvailability(context) == BleAvailability.READY },
        scope = viewModelScope,
        elapsedMillis = SystemClock::elapsedRealtime,
    )
    val state = discovery.state

    fun startScanning() = discovery.startDiscovery()
    fun stopScanning() = discovery.stopDiscovery()
    fun availabilityChanged() = discovery.availabilityChanged()
    override fun onCleared() = discovery.close()
}
