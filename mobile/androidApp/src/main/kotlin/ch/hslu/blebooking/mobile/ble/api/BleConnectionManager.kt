
package ch.hslu.blebooking.mobile.ble.api

import ch.hslu.blebooking.mobile.ble.gatt.BleGattSession
import ch.hslu.blebooking.mobile.ble.protocol.BleProtocolConstants

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattConnectionSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.UUID

class BleConnectionManager(
    private val context: Context
) {
    private val bluetoothManager =
        context.getSystemService(BluetoothManager::class.java)

    private val mainHandler = Handler(Looper.getMainLooper())

    private var gatt: BluetoothGatt? = null
    private var session: BleGattSession? = null

    private val listeners = mutableSetOf<BleConnectionListener>()

    fun addConnectionListener(listener: BleConnectionListener) {
        listeners.add(listener)
    }

    fun removeConnectionListener(listener: BleConnectionListener) {
        listeners.remove(listener)
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        checkMainThread()
        check(gatt == null) { "Connection already active" }

        val adapter = bluetoothManager.adapter
            ?: throw IllegalStateException("Bluetooth not supported")

        check(adapter.isEnabled) { "Bluetooth is disabled" }

        val device = adapter.getRemoteDevice(address)

        gatt = if (Build.VERSION.SDK_INT >= 37) {
            val settings = BluetoothGattConnectionSettings.Builder()
                .setAutoConnectEnabled(false)
                .setTransport(BluetoothDevice.TRANSPORT_LE)
                .build()

            device.connectGatt(
                settings,
                context.mainExecutor,
                gattCallback
            )
        } else {
            @Suppress("DEPRECATION")
            device.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        }

        if (gatt == null) {
            notifyError("Failed to start GATT connection")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        checkMainThread()
        gatt?.disconnect()
    }

    @SuppressLint("MissingPermission")
    fun close() {
        checkMainThread()

        val wasActive = gatt != null
        releaseConnection()

        if (wasActive) {
            notifyDisconnected()
        }
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {

        private fun deliver(gatt: BluetoothGatt, event: BleGattSession.Event) {
            mainHandler.post {
                if (this@BleConnectionManager.gatt === gatt) {
                    session?.events?.trySend(event)
                }
            }
        }

        @Deprecated("Used by Android before API 33")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            onCharacteristicRead(gatt, characteristic, characteristic.value?.copyOf() ?: byteArrayOf(), status)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            if (characteristic.uuid == UUID.fromString(BleProtocolConstants.TERMINAL_ID_UUID)) {
                deliver(gatt, BleGattSession.Event.Read(value.copyOf(), status))
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (characteristic.uuid == UUID.fromString(BleProtocolConstants.DOOR_COMMAND_UUID)) {
                deliver(gatt, BleGattSession.Event.Write(status))
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.characteristic.uuid == UUID.fromString(BleProtocolConstants.DOOR_STATUS_UUID) &&
                descriptor.uuid == UUID.fromString(BleProtocolConstants.CCCD_UUID)) {
                deliver(gatt, BleGattSession.Event.Subscription(status))
            }
        }

        @Deprecated("Used by Android before API 33")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            onCharacteristicChanged(gatt, characteristic, characteristic.value?.copyOf() ?: byteArrayOf())
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (characteristic.uuid == UUID.fromString(BleProtocolConstants.DOOR_STATUS_UUID)) {
                deliver(gatt, BleGattSession.Event.Notification(value.copyOf()))
            }
        }

        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            mainHandler.post {
                if (this@BleConnectionManager.gatt !== gatt) {
                    return@post
                }

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    releaseConnection()
                    notifyError("GATT connection error: $status")
                    return@post
                }

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        if (!gatt.discoverServices()) {
                            releaseConnection()
                            notifyError("Failed to start service discovery")
                        }
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        releaseConnection()
                        notifyDisconnected()
                    }
                }
            }
        }

        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int
        ) {
            mainHandler.post {
                if (this@BleConnectionManager.gatt !== gatt) {
                    return@post
                }

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    releaseConnection()
                    notifyError("Service discovery failed: $status")
                    return@post
                }

                val service = gatt.getService(
                    UUID.fromString(BleProtocolConstants.SERVICE_UUID)
                )

                val terminalInfo = service?.getCharacteristic(
                    UUID.fromString(BleProtocolConstants.TERMINAL_ID_UUID)
                )

                val doorCommand = service?.getCharacteristic(
                    UUID.fromString(BleProtocolConstants.DOOR_COMMAND_UUID)
                )

                val doorStatus = service?.getCharacteristic(
                    UUID.fromString(BleProtocolConstants.DOOR_STATUS_UUID)
                )

                val valid = terminalInfo != null &&
                        doorCommand != null &&
                        doorStatus != null &&
                        terminalInfo.properties and
                        BluetoothGattCharacteristic.PROPERTY_READ != 0 &&
                        doorCommand.properties and
                        BluetoothGattCharacteristic.PROPERTY_WRITE != 0 &&
                        doorStatus.properties and
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0

                if (!valid) {
                    releaseConnection()
                    notifyError("Required GATT characteristics missing")
                    return@post
                }

                val readySession = BleGattSession(
                    gatt,
                    terminalInfo!!,
                    doorCommand!!,
                    doorStatus!!,
                    closeConnection = {
                        if (this@BleConnectionManager.gatt === gatt) close()
                    }
                )

                session = readySession
                listeners.toList().forEach {
                    it.onReady(readySession)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun releaseConnection() {
        session?.invalidate()
        session = null

        gatt?.close()
        gatt = null
    }

    private fun notifyDisconnected() {
        listeners.toList().forEach {
            it.onDisconnected()
        }
    }

    private fun notifyError(message: String) {
        listeners.toList().forEach {
            it.onConnectionError(message)
        }
    }

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "BleConnectionManager must be used on the main thread"
        }
    }
}
