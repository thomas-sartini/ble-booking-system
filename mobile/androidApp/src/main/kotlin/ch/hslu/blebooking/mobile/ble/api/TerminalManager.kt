package ch.hslu.blebooking.mobile.ble.api

import ch.hslu.blebooking.mobile.ble.gatt.BleGattClient
import ch.hslu.blebooking.mobile.ble.gatt.BleGattSession
import ch.hslu.blebooking.mobile.ble.protocol.BleMessageCodec

import java.io.IOException
import java.util.UUID

/**
 * Executes the demo protocol defined in docs/ble-demo-protocol.md.
 * Operations are serialized per session and dispatched to the main thread.
 * The caller must obtain Bluetooth permissions before using this manager.
 * Each operation has a ten-second timeout after acquiring the session lock.
 * Failure, timeout or cancellation closes the session; commands are never retried.
 * If a command was sent without receiving its result, its outcome remains unknown.
 *
 * @param session Connected session supplied by [BleConnectionListener.onReady].
 */
class TerminalManager(private val session: BleGattSession) {
    private val gattClient = BleGattClient(session)

    /**
     * Reads the candidate's UUID; this does not authenticate the terminal.
     *
     * @return Non-zero Terminal ID encoded in the version 1 information payload.
     * @throws IOException If the payload is invalid or the GATT operation fails.
     */
    suspend fun readTerminalId(): UUID = gattClient.withGattOperation {
        val payload = gattClient.readTerminalInformation()
        BleMessageCodec.decodeTerminalId(payload)
    }

    /**
     * Releases the latch and waits for the matching successful Door Status notification.
     * This does not mean that the door is physically open.
     *
     * @throws IOException If GATT fails or the terminal reports a failure.
     */
    suspend fun openDoor() = executeDoorCommand(OPEN)

    /**
     * Requests locking after the door has been physically closed and awaits its result.
     *
     * @throws IOException If GATT fails or the terminal reports a failure, including an open door.
     */
    suspend fun closeDoor() = executeDoorCommand(CLOSE)

    /** Sends one command and checks the terminal's final result. */
    private suspend fun executeDoorCommand(command: Int) = gattClient.withGattOperation {
        gattClient.enableDoorStatusNotifications()
        val requestId = nextRequestId()
        writeDoorCommand(requestId, command)
        val result = awaitDoorResult(requestId, command)
        checkDoorResult(result)
    }

    /** Allocates an ID shared by all managers using this session; IDs are never reused. */
    private fun nextRequestId(): Int {
        if (session.nextRequestId > 0xffff) throw IOException("Request IDs exhausted; reconnect")
        return session.nextRequestId++
    }

    /** Encodes the request before passing its bytes to Android GATT. */
    private fun writeDoorCommand(requestId: Int, command: Int) {
        val payload = BleMessageCodec.encodeDoorCommand(requestId, command)
        gattClient.writeDoorCommand(payload)
    }

    /** Waits for both the write acknowledgement and a final result matching this request. */
    private suspend fun awaitDoorResult(requestId: Int, command: Int): Int {
        // Notifications can arrive before the write callback; retain both acknowledgements.
        var writeAcknowledged = false
        var result: Int? = null
        while (!writeAcknowledged || result == null) {
            when (val event = gattClient.awaitNextEvent()) {
                is BleGattSession.Event.Write -> {
                    gattClient.checkGattStatus(event.status)
                    writeAcknowledged = true
                }
                is BleGattSession.Event.Notification -> {
                    val status = BleMessageCodec.decodeDoorStatus(event.value)
                    if (status.requestId == requestId && status.command == command && status.result != 0) {
                        result = status.result
                    }
                }
                else -> Unit
            }
        }
        return result
    }

    /** Converts a terminal failure result into an error that the caller can display. */
    private fun checkDoorResult(result: Int) {
        if (result != 1) {
            throw IOException(when (result) {
                2 -> "Door command denied"
                3 -> "Please close the door before locking"
                4 -> "Terminal is busy"
                5 -> "Terminal actuator error"
                6 -> "Invalid door command"
                else -> "Invalid terminal result"
            })
        }
    }

    private companion object {
        const val OPEN = 1
        const val CLOSE = 2
    }
}
