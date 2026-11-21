package ch.hslu.blebooking.mobile.ble.protocol

import java.io.IOException
import java.nio.ByteBuffer
import java.util.UUID

/** Encodes and validates the version 1 demo payloads without depending on Android APIs. */
object BleMessageCodec {
    /**
     * Encodes a Door Command with a little-endian request ID.
     *
     * @param requestId Unsigned 16-bit identifier, unique within the current session.
     * @param command Protocol command: 1 for OPEN or 2 for CLOSE.
     * @return Four-byte Door Command payload.
     * @throws IllegalArgumentException If an argument is outside its protocol range.
     */
    fun encodeDoorCommand(requestId: Int, command: Int): ByteArray {
        require(requestId in 0..0xffff) { "Request ID must be an unsigned 16-bit value" }
        require(command in 1..2) { "Command must be OPEN (1) or CLOSE (2)" }
        // Layout: version, request ID low byte, request ID high byte, command.
        // For example, request ID 0x1234 is sent as 0x34 followed by 0x12.
        return byteArrayOf(1, requestId.toByte(), (requestId shr 8).toByte(), command.toByte())
    }

    /**
     * Decodes Terminal Information; the UUID uses network byte order.
     *
     * @param value Complete 17-byte Terminal Information payload.
     * @return Non-zero Terminal ID; this identifier does not authenticate the terminal.
     * @throws IOException If the length, version or Terminal ID is invalid.
     */
    fun decodeTerminalId(value: ByteArray): UUID {
        if (value.size != 17 || value[0] != 1.toByte()) {
            throw IOException("Invalid Terminal Information length or version")
        }
        // Skip the version byte. ByteBuffer reads big-endian by default, as required for the UUID.
        val bytes = ByteBuffer.wrap(value, 1, 16)
        // A UUID consists of two 64-bit halves: most significant first, then least significant.
        val id = UUID(bytes.long, bytes.long)
        if (id == UUID(0, 0)) throw IOException("Terminal ID must not be zero")
        return id
    }

    /**
     * Decodes and validates every field of a Door Status notification.
     * Matching the request and interpreting its result remain the caller's responsibility.
     *
     * @param value Complete seven-byte Door Status payload.
     * @return Validated status, including the reported latch and door states.
     * @throws IOException If the length, version or any field is invalid.
     */
    fun decodeDoorStatus(value: ByteArray): DoorStatus {
        if (value.size != 7 || value[0] != 1.toByte()) {
            throw IOException("Invalid Door Status length or version")
        }
        // Kotlin bytes are signed; masking with 0xff restores their unsigned values (0..255).
        // Shift the high byte left by 8 bits and combine it with the low byte to rebuild the ID.
        val id = (value[1].toInt() and 0xff) or ((value[2].toInt() and 0xff) shl 8)
        val command = value[3].toInt()
        val result = value[4].toInt()
        val latch = value[5].toInt()
        val door = value[6].toInt()
        if (command !in 1..2 || result !in 0..6 || latch !in 0..2 || door !in 0..2) {
            throw IOException("Invalid Door Status field value")
        }
        return DoorStatus(id, command, result, latch, door)
    }

    /**
     * Validated fields from a version 1 Door Status payload.
     *
     * @property requestId Unsigned 16-bit request identifier.
     * @property command 1 = OPEN; 2 = CLOSE.
     * @property result 0 = pending; 1 = success; 2 = denied; 3 = door not closed;
     * 4 = busy; 5 = actuator error; 6 = invalid command.
     * @property latch 0 = unknown; 1 = locked; 2 = released.
     * @property door 0 = unknown; 1 = closed; 2 = open.
     */
    data class DoorStatus(
        val requestId: Int,
        val command: Int,
        val result: Int,
        val latch: Int,
        val door: Int
    )
}
