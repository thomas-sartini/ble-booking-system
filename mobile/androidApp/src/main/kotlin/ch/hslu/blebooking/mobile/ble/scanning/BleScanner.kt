package ch.hslu.blebooking.mobile.ble.scanning

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import ch.hslu.blebooking.mobile.ble.common.*

/** Foreground discovery only. Advertisements identify candidates, not authenticated terminals. */
class BleScanner(context: Context) : TerminalScanner {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val listeners = mutableSetOf<BleScanListener>()
    private var scanner: BluetoothLeScanner? = null
    private var callback: ScanCallback? = null

    override fun addScanListener(listener: BleScanListener) { listeners.add(listener) }
    override fun removeScanListener(listener: BleScanListener) { listeners.remove(listener) }

    @SuppressLint("MissingPermission")
    override fun startScan() {
        if (callback != null) return
        check(bluetoothAvailability(context) == BleAvailability.READY) { "Bluetooth is unavailable" }
        val platformScanner = context.getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner
            ?: error("BLE scanner unavailable")
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) = publish(result)
            override fun onBatchScanResults(results: MutableList<ScanResult>) { results.forEach(::publish) }
            private fun publish(result: ScanResult) {
                main.post {
                    if (callback !== this || !BlePermissions.hasPermissions(context)) return@post
                    try {
                        val terminal = DiscoveredTerminal(result.scanRecord?.deviceName, result.device.address, result.rssi)
                        listeners.toList().forEach { it.onTerminalDiscovered(terminal) }
                    } catch (_: SecurityException) { onScanFailed(SCAN_FAILED_INTERNAL_ERROR) }
                }
            }
            override fun onScanFailed(errorCode: Int) {
                main.post {
                    if (callback !== this) return@post
                    stopScan()
                    listeners.toList().forEach { it.onScanFailed(errorCode) }
                }
            }
        }
        scanner = platformScanner
        callback = scanCallback
        try {
            platformScanner.startScan(
                listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(BleProtocolConstants.SERVICE_UUID)).build()),
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scanCallback)
        } catch (e: RuntimeException) {
            stopScan()
            throw e
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopScan() {
        val previous = callback
        callback = null // Ignore any late results from the old scan.
        try {
            if (previous != null) scanner?.stopScan(previous)
        } catch (_: SecurityException) {
            // Permissions may have been revoked during discovery.
        } catch (_: IllegalStateException) {
            // Adapter may have been switched off.
        } finally { scanner = null }
    }
}
