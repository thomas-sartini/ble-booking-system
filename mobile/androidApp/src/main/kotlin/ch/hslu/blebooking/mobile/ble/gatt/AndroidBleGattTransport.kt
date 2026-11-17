package ch.hslu.blebooking.mobile.ble.gatt

import ch.hslu.blebooking.mobile.ble.protocol.BleProtocolConstants

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import java.io.IOException
import java.util.UUID

/** Android implementation of the hardware calls; permission requests belong to the UI. */
@SuppressLint("MissingPermission")
internal class AndroidBleGattTransport(
    private val gatt: BluetoothGatt,
    private val terminalInformation: BluetoothGattCharacteristic,
    private val doorCommand: BluetoothGattCharacteristic,
    private val doorStatus: BluetoothGattCharacteristic
) : BleGattTransport {
    override fun readTerminalInformation(): Boolean = gatt.readCharacteristic(terminalInformation)

    override fun writeDoorCommand(payload: ByteArray): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            gatt.writeCharacteristic(doorCommand, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) ==
                BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            doorCommand.value = payload
            doorCommand.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(doorCommand)
        }
    }

    override fun enableDoorStatusNotifications(): Boolean {
        val descriptor = doorStatus.getDescriptor(UUID.fromString(BleProtocolConstants.CCCD_UUID))
            ?: throw IOException("Door Status CCCD missing")
        // Enable Android delivery before asking the terminal to send notifications.
        if (!gatt.setCharacteristicNotification(doorStatus, true)) return false
        return if (Build.VERSION.SDK_INT >= 33) {
            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ==
                BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }
    }
}
