package ch.hslu.blebooking.mobile.ble.api

import ch.hslu.blebooking.mobile.ble.protocol.BleProtocolConstants

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import ch.hslu.blebooking.mobile.ble.model.DiscoveredTerminal
import java.util.UUID

/**
 * Discovers candidates by Service UUID; identification and authentication happen after connection.
 *
 * @param context Android context used to obtain the Bluetooth service and application context.
 */
class BleScanner(
    val context: Context
) {
    private val appContext = context.applicationContext
    private val listeners = mutableListOf<BleScanListener>()
    private  val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private var scanner: BluetoothLeScanner? = null
    private val scanCallBack = object : ScanCallback(){
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (!BlePermissions.hasPermissions(appContext)) return
            val name = result.scanRecord?.deviceName
            val address = result.device.address
            val rssi = result.rssi
            val terminal = DiscoveredTerminal(
                name = name,
                address = address,
                rssi = rssi
            )
            notifyListeners(terminal)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleScanner", "BLE scan failed: $errorCode")
            listeners.toList().forEach { it.onScanFailed(errorCode) }
        }

    }

    /**
     * Requires granted permissions and enabled Bluetooth; does not show permission dialogs.
     * */
    @SuppressLint("MissingPermission") // Permissions are checked before accessing Bluetooth APIs.
    fun startScan(){
        if (!BlePermissions.hasPermissions(appContext)) {
            throw SecurityException("BLE permissions must be granted before starting a scan")
        }
        val adapter = bluetoothManager.adapter ?: throw IllegalStateException("Bluetooth not supported")
        scanner = adapter.bluetoothLeScanner ?: throw IllegalStateException("BLE Scanner unavailable")
        val filter = ScanFilter.Builder()
        filter.setServiceUuid(ParcelUuid(UUID.fromString(BleProtocolConstants.SERVICE_UUID)))
        scanner?.startScan(
            listOf(filter.build()),
            ScanSettings.Builder().build(),
            scanCallBack
        )
    }

    @SuppressLint("MissingPermission")
    fun stopScan(){
        // Release the local reference even if permissions no longer allow stopping the scan.
        if (BlePermissions.hasPermissions(appContext)) {
            scanner?.stopScan(scanCallBack)
        }
        scanner = null
    }

    /**
     * Registers a listener for subsequent scan observations.
     *
     * @param listener Callback recipient; remove it when observations are no longer needed.
     */
    fun addScanListener(listener: BleScanListener){
        listeners.add(listener)
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener The same listener instance passed to [addScanListener].
     */
    fun removeScanListener(listener: BleScanListener){
        listeners.remove(listener)
    }

    private fun notifyListeners(terminal: DiscoveredTerminal){
        listeners.forEach { listener ->
            listener.onTerminalDiscovered(terminal)
        }
    }
}
