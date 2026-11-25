package ch.hslu.blebooking.mobile.ble.scanning

/** Radio operations used by discovery; implementations deliver callbacks on the main thread. */
interface TerminalScanner {
    fun addScanListener(listener: BleScanListener)
    fun removeScanListener(listener: BleScanListener)
    fun startScan()
    fun stopScan()
}
