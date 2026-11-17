package ch.hslu.blebooking.mobile.ble

import ch.hslu.blebooking.mobile.ble.api.TerminalManager
import ch.hslu.blebooking.mobile.ble.gatt.BleGattSession
import ch.hslu.blebooking.mobile.ble.gatt.BleGattTransport

import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Runs real manager/client coroutines against a controllable simulated terminal. */
@OptIn(ExperimentalCoroutinesApi::class)
class TerminalManagerRuntimeTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var terminal: SimulatedTerminal
    private lateinit var manager: TerminalManager

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        terminal = SimulatedTerminal()
        manager = TerminalManager(terminal.session)
    }

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `reads terminal identity through an asynchronous callback`() = runTest(dispatcher) {
        val read = async { manager.readTerminalId() }
        runCurrent()
        assertFalse(read.isCompleted)
        terminal.emit(BleGattSession.Event.Read(byteArrayOf(1, 0x55, 0x0e, 0x84.toByte(), 0,
            0xe2.toByte(), 0x9b.toByte(), 0x41, 0xd4.toByte(), 0xa7.toByte(), 0x16,
            0x44, 0x66, 0x55, 0x44, 0, 0), 0))
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), read.await())
    }

    @Test fun `waits for subscription before sending and reuses it for close`() = runTest(dispatcher) {
        terminal.autoSubscribe = false
        val open = async { manager.openDoor() }
        runCurrent()
        assertTrue(terminal.writes.isEmpty())
        terminal.emit(BleGattSession.Event.Subscription(0))
        runCurrent()
        assertArrayEquals(byteArrayOf(1, 0, 0, 1), terminal.writes.single())
        terminal.reply(0, 1)
        open.await()
        val close = async { manager.closeDoor() }
        runCurrent()
        assertArrayEquals(byteArrayOf(1, 1, 0, 2), terminal.writes.last())
        terminal.reply(1, 2)
        close.await()
        assertEquals(1, terminal.subscriptions)
    }

    @Test fun `write acknowledgement alone does not complete a command`() = runTest(dispatcher) {
        val open = async { manager.openDoor() }
        runCurrent()
        terminal.emit(BleGattSession.Event.Write(0))
        runCurrent()
        assertFalse(open.isCompleted)
        terminal.status(0, 1)
        open.await()
    }

    @Test fun `accepts result arriving before write acknowledgement`() = runTest(dispatcher) {
        val open = async { manager.openDoor() }
        runCurrent()
        terminal.status(0, 1)
        runCurrent()
        assertFalse(open.isCompleted)
        terminal.emit(BleGattSession.Event.Write(0))
        open.await()
    }

    @Test fun `ignores other request IDs commands and pending results`() = runTest(dispatcher) {
        val open = async { manager.openDoor() }
        runCurrent()
        terminal.emit(BleGattSession.Event.Write(0))
        terminal.status(22, 1)
        terminal.status(0, 2)
        terminal.status(0, 1, 0)
        runCurrent()
        assertFalse(open.isCompleted)
        terminal.status(0, 1)
        open.await()
    }

    @Test fun `reports door not closed without retrying`() = runTest(dispatcher) {
        val close = async { runCatching { manager.closeDoor() } }
        runCurrent()
        terminal.reply(0, 2, 3)
        assertEquals("Please close the door before locking", close.await().exceptionOrNull()?.message)
        assertEquals(1, terminal.writes.size)
        assertFalse(terminal.session.isActive)
    }

    @Test fun `timeout closes session and never resends`() = runTest(dispatcher) {
        val open = async { runCatching { manager.openDoor() } }
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        assertTrue(open.await().exceptionOrNull() is TimeoutCancellationException)
        assertEquals(1, terminal.closes)
        assertEquals(1, terminal.writes.size)
    }

    @Test fun `disconnect fails a pending command with unknown outcome`() = runTest(dispatcher) {
        val open = async { runCatching { manager.openDoor() } }
        runCurrent()
        terminal.emit(BleGattSession.Event.Write(0))
        runCurrent()
        terminal.session.invalidate()
        val error = open.await().exceptionOrNull()
        assertTrue(error is IOException)
        assertTrue(error!!.message!!.contains("unknown"))
        assertEquals(1, terminal.writes.size)
    }

    @Test fun `cancelling an active command closes the session`() = runTest(dispatcher) {
        val open = launch { manager.openDoor() }
        runCurrent()
        open.cancel()
        open.join()
        assertEquals(1, terminal.closes)
        assertFalse(terminal.session.isActive)
    }

    @Test fun `two managers serialize commands and share request IDs`() = runTest(dispatcher) {
        val secondManager = TerminalManager(terminal.session)
        val open = async { manager.openDoor() }
        val close = async { secondManager.closeDoor() }
        runCurrent()
        assertEquals(1, terminal.writes.size)
        terminal.reply(0, 1)
        open.await()
        runCurrent()
        assertEquals(2, terminal.writes.size)
        assertArrayEquals(byteArrayOf(1, 1, 0, 2), terminal.writes.last())
        terminal.reply(1, 2)
        close.await()
    }

    @Test fun `cancelling a queued command leaves the active one running`() = runTest(dispatcher) {
        val open = async { manager.openDoor() }
        runCurrent()
        val close = launch { manager.closeDoor() }
        runCurrent()
        close.cancel()
        close.join()
        assertTrue(terminal.session.isActive)
        terminal.reply(0, 1)
        open.await()
        assertEquals(1, terminal.writes.size)
    }

    @Test fun `rejected write fails immediately`() = runTest(dispatcher) {
        terminal.acceptWrite = false
        val result = runCatching { manager.openDoor() }
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(1, terminal.closes)
    }

    @Test fun `failed subscription prevents command from being sent`() = runTest(dispatcher) {
        terminal.autoSubscribe = false
        val open = async { runCatching { manager.openDoor() } }
        runCurrent()
        terminal.emit(BleGattSession.Event.Subscription(133))
        assertTrue(open.await().exceptionOrNull() is IOException)
        assertTrue(terminal.writes.isEmpty())
    }

    @Test fun `invalid terminal identity closes the connection`() = runTest(dispatcher) {
        val read = async { runCatching { manager.readTerminalId() } }
        runCurrent()
        terminal.emit(BleGattSession.Event.Read(byteArrayOf(1), 0))
        assertTrue(read.await().exceptionOrNull() is IOException)
        assertFalse(terminal.session.isActive)
    }

    @Test fun `malformed notification fails the pending command`() = runTest(dispatcher) {
        val open = async { runCatching { manager.openDoor() } }
        runCurrent()
        terminal.emit(BleGattSession.Event.Notification(byteArrayOf(1)))
        assertTrue(open.await().exceptionOrNull() is IOException)
        assertFalse(terminal.session.isActive)
    }

    @Test fun `failed write callback overrides a successful notification`() = runTest(dispatcher) {
        val open = async { runCatching { manager.openDoor() } }
        runCurrent()
        terminal.status(0, 1)
        terminal.emit(BleGattSession.Event.Write(133))
        assertTrue(open.await().exceptionOrNull() is IOException)
    }

    @Test fun `closed session refuses new commands`() = runTest(dispatcher) {
        terminal.session.invalidate()
        assertTrue(runCatching { manager.openDoor() }.exceptionOrNull() is IllegalStateException)
        assertTrue(terminal.writes.isEmpty())
    }

    /** Emits callback events explicitly so each test controls their timing and order. */
    private class SimulatedTerminal : BleGattTransport {
        val session = BleGattSession(this, ::closeSession)
        private fun closeSession() { closes++; session.invalidate() }
        val writes = mutableListOf<ByteArray>()
        var subscriptions = 0
        var closes = 0
        var autoSubscribe = true
        var acceptWrite = true

        override fun readTerminalInformation() = true
        override fun writeDoorCommand(payload: ByteArray): Boolean {
            writes.add(payload.copyOf())
            return acceptWrite
        }
        override fun enableDoorStatusNotifications(): Boolean {
            subscriptions++
            if (autoSubscribe) emit(BleGattSession.Event.Subscription(0))
            return true
        }
        fun emit(event: BleGattSession.Event) { check(session.events.trySend(event).isSuccess) }
        fun status(id: Int, command: Int, result: Int = 1) {
            // Build peripheral payloads independently of the production codec.
            emit(BleGattSession.Event.Notification(byteArrayOf(1, id.toByte(), (id shr 8).toByte(),
                command.toByte(), result.toByte(), 2, 1)))
        }
        fun reply(id: Int, command: Int, result: Int = 1) {
            emit(BleGattSession.Event.Write(0))
            status(id, command, result)
        }
    }
}
