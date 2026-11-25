package ch.hslu.blebooking.mobile.ui.screens.home

import android.bluetooth.BluetoothAdapter
import android.content.*
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import ch.hslu.blebooking.mobile.ble.common.*

data class HomeBluetoothState(val availability: BleAvailability, val resolve: () -> Unit)

/** Home owns permission prompts; scanning never opens a permission dialog. */
@Composable
fun rememberHomeBluetoothState(): HomeBluetoothState {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var availability by remember { mutableStateOf(bluetoothAvailability(context)) }
    var requested by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        availability = bluetoothAvailability(context)
    }
    val enableLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        availability = bluetoothAvailability(context)
    }
    DisposableEffect(context, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) availability = bluetoothAvailability(context)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { availability = bluetoothAvailability(context) }
        }
        lifecycle.addObserver(observer)
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED).apply { addAction(LocationManager.MODE_CHANGED_ACTION) }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose { lifecycle.removeObserver(observer); context.unregisterReceiver(receiver) }
    }
    return HomeBluetoothState(availability) {
        when (availability) {
            BleAvailability.PERMISSION_REQUIRED -> {
                if (!requested) {
                    requested = true
                    permissionLauncher.launch(BlePermissions.requiredPermissions())
                } else {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")))
                }
            }
            BleAvailability.BLUETOOTH_OFF -> enableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            BleAvailability.LOCATION_OFF -> context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            else -> Unit
        }
    }
}
