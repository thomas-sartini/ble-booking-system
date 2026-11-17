package ch.hslu.blebooking.mobile.ble.gatt

import android.bluetooth.BluetoothGatt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException

/**
 * Handles Android GATT calls, callbacks and operation lifetime for one terminal session.
 * Call read, write and notification methods inside [withGattOperation].
 *
 * @param session Connected GATT session shared with the terminal manager.
 */
internal class BleGattClient(private val session: BleGattSession) {
    /** Reads the raw Terminal Information payload and checks the GATT response. */
    suspend fun readTerminalInformation(): ByteArray {
        requireStarted(session.transport.readTerminalInformation())
        val response = awaitEvent<BleGattSession.Event.Read>()
        checkGattStatus(response.status)
        return response.value
    }

    /** Starts a command write; its acknowledgement and door result arrive separately. */
    fun writeDoorCommand(payload: ByteArray) {
        requireStarted(session.transport.writeDoorCommand(payload))
    }

    /** Waits for the next GATT callback without blocking the main thread. */
    suspend fun awaitNextEvent(): BleGattSession.Event = session.events.receive()

    /** Enables Door Status notifications once per session and waits for the terminal's acknowledgement. */
    suspend fun enableDoorStatusNotifications() {
        if (session.notificationsEnabled) return
        requireStarted(session.transport.enableDoorStatusNotifications())
        checkGattStatus(awaitEvent<BleGattSession.Event.Subscription>().status)
        session.notificationsEnabled = true
    }

    /** Waits for a callback event of type [T], discarding other events without blocking the thread. */
    private suspend inline fun <reified T : BleGattSession.Event> awaitEvent(): T {
        while (true) {
            val event = session.events.receive()
            if (event is T) return event
        }
    }

    /**
     * Runs one operation at a time on the main thread, with a ten-second timeout.
     * Closes the session if the operation fails or is cancelled.
     *
     * @param block Read or command sequence to execute while holding the session lock.
     * @return The value produced by [block].
     */
    suspend fun <T> withGattOperation(block: suspend () -> T): T = withContext(Dispatchers.Main.immediate) {
        session.operations.withLock {
            check(session.isActive) { "GATT session is no longer active" }
            while (session.events.tryReceive().isSuccess) { /* Discard unsolicited idle notifications. */ }
            try {
                withTimeout(10_000) { block() }
            } catch (error: Exception) {
                // A cancelled or timed-out GATT request cannot safely be reused or retried.
                // Closing also prevents a late callback from completing a later operation.
                session.close()
                throw error
            }
        }
    }

    /** Checks whether Android accepted the GATT request; completion arrives in a later callback. */
    private fun requireStarted(started: Boolean) {
        if (!started) throw IOException("Failed to start GATT operation")
    }

    /** Checks the GATT callback status, not the door command's application-level result. */
    fun checkGattStatus(status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) throw IOException("GATT operation failed: $status")
    }
}
