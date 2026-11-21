package ch.hslu.blebooking.mobile.ble.gatt

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import java.io.IOException

class BleGattSession internal constructor(
    internal val transport: BleGattTransport,
    private val closeConnection: () -> Unit
) {
    internal constructor(
        gatt: BluetoothGatt,
        terminalInformation: BluetoothGattCharacteristic,
        doorCommand: BluetoothGattCharacteristic,
        doorStatus: BluetoothGattCharacteristic,
        closeConnection: () -> Unit
    ) : this(AndroidBleGattTransport(gatt, terminalInformation, doorCommand, doorStatus), closeConnection)

    internal val operations = Mutex()
    internal val events = Channel<Event>(Channel.UNLIMITED)
    internal var notificationsEnabled = false
    internal var nextRequestId = 0

    internal sealed interface Event {
        data class Read(val value: ByteArray, val status: Int) : Event
        data class Write(val status: Int) : Event
        data class Subscription(val status: Int) : Event
        data class Notification(val value: ByteArray) : Event
    }

    var isActive: Boolean = true
        private set

    internal fun invalidate() {
        isActive = false
        events.close(IOException("GATT connection closed; pending command outcome is unknown"))
    }

    internal fun close() = closeConnection()
}
