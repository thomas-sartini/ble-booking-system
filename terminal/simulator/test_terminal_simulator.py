import asyncio
import unittest
from uuid import UUID
from unittest.mock import AsyncMock
from bumble import att
from bumble.device import Device
from terminal_simulator import Terminal, advertising, SERVICE

class ProtocolTest(unittest.IsolatedAsyncioTestCase):
    def terminal(self, scenario='success'):
        device = Device()
        device.notify_subscriber = AsyncMock()
        terminal = Terminal(device, UUID('550e8400-e29b-41d4-a716-446655440000'), 0, scenario)
        return terminal, device

    async def test_id_and_open_close(self):
        terminal, device = self.terminal()
        self.assertEqual(terminal.read_id(None).hex(), '01550e8400e29b41d4a716446655440000')
        for command, latch in [(1, 2), (2, 1)]:
            terminal.write(None, bytes([1, 0x34, 0x12, command]))
            await terminal.task
            self.assertEqual(terminal.last_status, bytes([1, 0x34, 0x12, command, 1, latch, 1]))
        self.assertEqual(device.notify_subscriber.await_count, 4)

    async def test_failures(self):
        for scenario, result in [('door-open', 3), ('denied', 2), ('busy', 4), ('actuator-error', 5)]:
            terminal, _ = self.terminal(scenario)
            terminal.write(None, bytes([1, 7, 0, 2]))
            await terminal.task
            self.assertEqual(terminal.last_status[4], result)

    async def test_timeout_disconnect_and_invalid_write(self):
        terminal, device = self.terminal('timeout')
        terminal.write(None, bytes([1, 0, 0, 1]))
        await terminal.task
        self.assertEqual(terminal.last_status[4], 0)
        self.assertTrue(terminal.locked)
        with self.assertRaises(att.ATT_Error):
            terminal.write(None, b'bad')
        with self.assertRaises(att.ATT_Error):
            terminal.write(None, bytes([2, 0, 0, 1]))
        terminal, _ = self.terminal('disconnect')
        connection = AsyncMock()
        terminal.write(connection, bytes([1, 0, 0, 1]))
        await terminal.task
        connection.disconnect.assert_awaited_once()

    def test_advertising(self):
        adv, response = advertising('Locker 23')
        self.assertEqual(adv, bytes.fromhex('0201061107') + UUID(SERVICE).bytes[::-1])
        self.assertEqual(response, b'\x0a\x09Locker 23')
        with self.assertRaises(ValueError):
            advertising('x' * 30)

if __name__ == '__main__':
    unittest.main()
