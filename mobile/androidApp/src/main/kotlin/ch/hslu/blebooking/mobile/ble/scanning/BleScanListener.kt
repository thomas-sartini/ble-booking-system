package ch.hslu.blebooking.mobile.ble.scanning

import ch.hslu.blebooking.mobile.ble.common.DiscoveredTerminal

interface BleScanListener {
    fun onScanFailed(errorCode: Int)
    fun onTerminalDiscovered(terminal: DiscoveredTerminal)
}
