package ch.hslu.blebooking.mobile.ble

import ch.hslu.blebooking.mobile.ble.protocol.BleMessageCodec

import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException
import java.util.UUID

class BleMessageCodecTest {
    @Test
    fun `reads UUID in network byte order`() {
        val payload = byteArrayOf(1, 0x55, 0x0e, 0x84.toByte(), 0, 0xe2.toByte(), 0x9b.toByte(),
            0x41, 0xd4.toByte(), 0xa7.toByte(), 0x16, 0x44, 0x66, 0x55, 0x44, 0, 0)
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            BleMessageCodec.decodeTerminalId(payload))
    }

    @Test
    fun `rejects invalid terminal information`() {
        for (payload in listOf(ByteArray(16), ByteArray(18), ByteArray(17).apply { this[0] = 2 },
            ByteArray(17).apply { this[0] = 1 })) {
            assertThrows(IOException::class.java) { BleMessageCodec.decodeTerminalId(payload) }
        }
    }

    @Test
    fun `encodes both commands and unsigned request boundaries`() {
        assertArrayEquals(byteArrayOf(1, 0x34, 0x12, 1), BleMessageCodec.encodeDoorCommand(0x1234, 1))
        assertArrayEquals(byteArrayOf(1, 0, 0, 2), BleMessageCodec.encodeDoorCommand(0, 2))
        assertArrayEquals(byteArrayOf(1, -1, -1, 2), BleMessageCodec.encodeDoorCommand(0xffff, 2))
        for ((id, command) in listOf(-1 to 1, 65536 to 1, 0 to 0, 0 to 3)) {
            assertThrows(IllegalArgumentException::class.java) { BleMessageCodec.encodeDoorCommand(id, command) }
        }
    }

    @Test
    fun `decodes every status field including unsigned request ID`() {
        assertEquals(BleMessageCodec.DoorStatus(0x9234, 1, 1, 2, 1),
            BleMessageCodec.decodeDoorStatus(byteArrayOf(1, 0x34, 0x92.toByte(), 1, 1, 2, 1)))
    }

    @Test
    fun `preserves pending and failure results`() {
        for (result in 0..6) {
            assertEquals(result, BleMessageCodec.decodeDoorStatus(
                byteArrayOf(1, 0x36, 0x12, 2, result.toByte(), 2, 2)).result)
        }
    }

    @Test
    fun `rejects malformed status`() {
        for (payload in listOf(byteArrayOf(), ByteArray(8), byteArrayOf(2, 0, 0, 1, 1, 2, 1),
            byteArrayOf(1, 0, 0, 0, 1, 2, 1), byteArrayOf(1, 0, 0, 3, 1, 2, 1),
            byteArrayOf(1, 0, 0, 1, 7, 2, 1), byteArrayOf(1, 0, 0, 1, 1, 3, 1),
            byteArrayOf(1, 0, 0, 1, 1, 2, 3))) {
            assertThrows(IOException::class.java) { BleMessageCodec.decodeDoorStatus(payload) }
        }
    }
}
