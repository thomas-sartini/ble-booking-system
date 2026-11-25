package ch.hslu.blebooking.mobile.ble.scanning

import ch.hslu.blebooking.mobile.ble.common.DiscoveredTerminal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalDiscoveryManagerTest {
    private class FakeScanner : TerminalScanner {
        var listener: BleScanListener? = null
        var running = false
        var starts = 0
        override fun addScanListener(listener: BleScanListener) { this.listener = listener }
        override fun removeScanListener(listener: BleScanListener) { this.listener = null }
        override fun startScan() { running = true; starts++ }
        override fun stopScan() { running = false }
        fun emit(name: String? = "Room", address: String = "A", rssi: Int = -50) {
            listener?.onTerminalDiscovered(DiscoveredTerminal(name, address, rssi))
        }
    }

    @Test fun updatesDuplicatesExpiresAndRediscovers() = runTest {
        val scanner = FakeScanner()
        val manager = TerminalDiscoveryManager(scanner, { true }, this, { testScheduler.currentTime })
        manager.startDiscovery()
        manager.startDiscovery()
        assertEquals(1, scanner.starts)
        scanner.emit()
        advanceTimeBy(4_000); runCurrent()
        scanner.emit(name = null, rssi = -70)
        assertEquals(1, manager.state.value.terminals.size)
        assertEquals("Room", manager.state.value.terminals.single().name)
        assertEquals(-70, manager.state.value.terminals.single().rssi)
        advanceTimeBy(5_000); runCurrent()
        assertEquals(1, manager.state.value.terminals.size)
        advanceTimeBy(1_000); runCurrent()
        assertTrue(manager.state.value.terminals.isEmpty())
        scanner.emit()
        assertEquals(1, manager.state.value.terminals.size)
        manager.close()
        assertFalse(scanner.running)
        assertNull(scanner.listener)
    }

    @Test fun refreshUpdatesOnlySelectedSignalAndTimesOut() = runTest {
        val scanner = FakeScanner()
        val manager = TerminalDiscoveryManager(scanner, { true }, this, { testScheduler.currentTime })
        manager.selectTerminal(DiscoveredTerminal("Original", "A", -50))
        manager.refreshSignal()
        scanner.emit(address = "B")
        assertTrue(manager.state.value.isRefreshingSignal)
        scanner.emit(name = "Changed", rssi = -90)
        assertEquals("Original", manager.state.value.selectedTerminal?.name)
        assertEquals(-90, manager.state.value.selectedTerminal?.rssi)
        assertFalse(manager.state.value.isRefreshingSignal)
        manager.refreshSignal()
        advanceTimeBy(5_000); runCurrent()
        assertTrue(manager.state.value.signalNotDetected)
        assertFalse(scanner.running)
        manager.refreshSignal()
        scanner.emit()
        assertFalse(manager.state.value.signalNotDetected)
        manager.close()
    }

    @Test fun unavailableAndFailureStopRadioAndMonitoring() = runTest {
        val scanner = FakeScanner()
        var available = true
        val manager = TerminalDiscoveryManager(scanner, { available }, this, { testScheduler.currentTime })
        manager.startDiscovery()
        scanner.emit()
        available = false
        advanceTimeBy(1_000); runCurrent()
        assertFalse(scanner.running)
        assertTrue(manager.state.value.terminals.isEmpty())
        assertEquals(DiscoveryError.Unavailable, manager.state.value.error)
        available = true
        manager.startDiscovery()
        scanner.listener?.onScanFailed(2)
        assertEquals(DiscoveryError.ScanFailed(2), manager.state.value.error)
        assertFalse(manager.state.value.isScanning)
        manager.close()
    }

    @Test fun stoppingIgnoresLateResultsAndCancelsExpiry() = runTest {
        val scanner = FakeScanner()
        val manager = TerminalDiscoveryManager(scanner, { true }, this, { testScheduler.currentTime })
        manager.startDiscovery()
        scanner.emit()
        manager.stopDiscovery()
        scanner.emit(address = "B")
        advanceUntilIdle()
        assertEquals(listOf("A"), manager.state.value.terminals.map { it.address })
        assertFalse(scanner.running)
        manager.close()
    }
}
