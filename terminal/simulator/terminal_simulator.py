"""BLE demo peripheral. Run with android-netsim or a supported USB HCI transport."""
import argparse
import asyncio
import logging
from uuid import UUID

from bumble import att, hci
from bumble.device import Device, DeviceConfiguration
from bumble.gatt import Characteristic, CharacteristicValue, Service
from bumble.transport import open_transport

SERVICE = 'fd698d10-c5c7-429b-9fb3-18a41dd73998'
INFORMATION = '81f905bd-4d25-4eeb-bf1d-89e909368fae'
COMMAND = '08d0c22b-0be0-45c1-aea4-7d181b40f733'
STATUS = '3b33ab4f-826c-497b-a653-b16c46425d73'
LOG = logging.getLogger('terminal')


def advertising(name):
    encoded = name.encode('utf-8')
    if len(encoded) > 29:
        raise ValueError('Name must fit in 29 UTF-8 bytes')
    return (bytes.fromhex('0201061107') + UUID(SERVICE).bytes[::-1],
            bytes([len(encoded) + 1, 9]) + encoded)


class Terminal:
    def __init__(self, device, terminal_id, delay=0.5, scenario='success'):
        if terminal_id.int == 0:
            raise ValueError('Terminal ID must not be zero')
        self.device = device
        self.terminal_id = terminal_id
        self.delay = delay
        self.scenario = scenario
        self.locked = scenario != 'door-open'
        self.door_open = scenario == 'door-open'
        self.task = None
        self.last_status = bytes([1, 0, 0, 1, 0, 1, 1])
        self.status = Characteristic(STATUS, Characteristic.Properties.READ | Characteristic.Properties.NOTIFY,
                                     att.Attribute.Permissions.READABLE,
                                     CharacteristicValue(read=lambda _: self.last_status))
        device.add_service(Service(SERVICE, [
            Characteristic(INFORMATION, Characteristic.Properties.READ, att.Attribute.Permissions.READABLE,
                           CharacteristicValue(read=self.read_id)),
            Characteristic(COMMAND, Characteristic.Properties.WRITE, att.Attribute.Permissions.WRITEABLE,
                           CharacteristicValue(write=self.write)),
            self.status,
        ]))
        device.on('connection', self.connected)

    def read_id(self, connection):
        value = b'\x01' + self.terminal_id.bytes
        LOG.info('READ ID %s', value.hex(' '))
        return value

    def connected(self, connection):
        LOG.info('CONNECTED %s', connection.peer_address)
        connection.on('disconnection', self.disconnected)

    def disconnected(self, reason):
        LOG.info('DISCONNECTED reason=%s', reason)
        if self.task:
            self.task.cancel()
            self.task = None

    def write(self, connection, value):
        LOG.info('WRITE %s', value.hex(' '))
        if len(value) != 4:
            raise att.ATT_Error(att.ATT_INVALID_ATTRIBUTE_LENGTH_ERROR)
        if value[0] != 1:
            raise att.ATT_Error(att.ErrorCode.VALUE_NOT_ALLOWED)
        # The callback returns immediately so the ATT write acknowledgement is independent.
        if self.task and not self.task.done():
            raise att.ATT_Error(att.ErrorCode.UNLIKELY_ERROR)
        self.task = asyncio.create_task(self.complete(connection, bytes(value)))
        self.task.add_done_callback(self.finished)

    @staticmethod
    def finished(task):
        if not task.cancelled() and task.exception():
            LOG.error('Command failed: %s', task.exception())

    async def notify(self, connection, request, result):
        self.last_status = bytes([1, request[1], request[2], request[3], result,
                                  1 if self.locked else 2, 2 if self.door_open else 1])
        LOG.info('NOTIFY %s', self.last_status.hex(' '))
        await self.device.notify_subscriber(connection, self.status, self.last_status)

    async def complete(self, connection, request):
        command = request[3]
        if command not in (1, 2):
            await self.notify(connection, request, 6)
            return
        await self.notify(connection, request, 0)
        await asyncio.sleep(self.delay)
        if self.scenario == 'timeout':
            LOG.info('Suppressing final result to simulate timeout')
            return
        if self.scenario == 'disconnect':
            await connection.disconnect()
            return
        result = {'denied': 2, 'busy': 4, 'actuator-error': 5}.get(self.scenario, 1)
        if result == 1 and command == 2 and self.door_open:
            result = 3
        if result == 1:
            self.locked = command == 2
        await self.notify(connection, request, result)


async def run(args):
    adv, response = advertising(args.name)
    async with await open_transport(args.transport) as transport:
        config = DeviceConfiguration(name=args.name, address=hci.Address(args.address),
                                     advertising_interval_min=100, advertising_interval_max=100)
        device = Device.from_config_with_hci(config, transport.source, transport.sink)
        terminal = Terminal(device, UUID(args.id), args.delay, args.scenario)
        await device.power_on()
        await device.start_advertising(advertising_data=adv, scan_response_data=response, auto_restart=True)
        LOG.info('Advertising %s | ID %s | scenario %s', args.name, args.id, args.scenario)
        try:
            await asyncio.Future()
        finally:
            if terminal.task:
                terminal.task.cancel()
                await asyncio.gather(terminal.task, return_exceptions=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--transport', default='android-netsim')
    parser.add_argument('--name', default='Locker 23')
    parser.add_argument('--address', default='F0:F1:F2:F3:F4:F5')
    parser.add_argument('--id', default='550e8400-e29b-41d4-a716-446655440000')
    parser.add_argument('--delay', type=float, default=0.5)
    parser.add_argument('--scenario', choices=['success', 'door-open', 'denied', 'busy', 'actuator-error', 'timeout', 'disconnect'], default='success')
    args = parser.parse_args()
    if args.delay < 0:
        parser.error('--delay must not be negative')
    logging.basicConfig(level=logging.INFO, format='%(asctime)s %(name)s: %(message)s')
    try:
        asyncio.run(run(args))
    except KeyboardInterrupt:
        pass
    except Exception as error:
        parser.exit(1, f'Terminal could not run: {error}\nFor Netsim install requirements.txt and start the Android emulator first.\n')


if __name__ == '__main__':
    main()
