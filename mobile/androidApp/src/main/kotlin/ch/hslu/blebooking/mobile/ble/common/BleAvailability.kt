package ch.hslu.blebooking.mobile.ble.common

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build

enum class BleAvailability { READY, UNSUPPORTED, PERMISSION_REQUIRED, BLUETOOTH_OFF, LOCATION_OFF }

@SuppressLint("MissingPermission")
fun bluetoothAvailability(context: Context): BleAvailability {
    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) return BleAvailability.UNSUPPORTED
    if (!BlePermissions.hasPermissions(context)) return BleAvailability.PERMISSION_REQUIRED
    return try {
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: return BleAvailability.UNSUPPORTED
        when {
            !adapter.isEnabled -> BleAvailability.BLUETOOTH_OFF
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
                context.getSystemService(LocationManager::class.java)?.isLocationEnabled != true -> BleAvailability.LOCATION_OFF
            else -> BleAvailability.READY
        }
    } catch (_: SecurityException) { BleAvailability.PERMISSION_REQUIRED }
}
