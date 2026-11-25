package ch.hslu.blebooking.mobile.ble.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Checks permissions only; the UI is responsible for requesting them before scanning. */
object BlePermissions {
    // Pass these to ActivityResultContracts.RequestMultiplePermissions from the UI.
    fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // CONNECT is also required by the scanner's Bluetooth adapter access.
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // Android 10 and 11 require location permission for BLE discovery.
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    /**
     * Checks whether all permissions required by the scanner are currently granted.
     *
     * @param context Android context used to check the application's permission grants.
     * @return True only when every permission returned by [requiredPermissions] is granted.
     */
    fun hasPermissions(context: Context): Boolean =
        requiredPermissions().all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
}
