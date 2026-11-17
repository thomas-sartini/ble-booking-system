# Bumble Terminal Simulator

Implements `docs/ble-demo-protocol.md` outside the Android app. No actuator, authentication, or booking authorization is implemented.

Run all commands from the repository root in PowerShell.

## Setup

```powershell
.\.venv-bumble\Scripts\python.exe -m pip install -r terminal\simulator\requirements.txt
```

The Android extra is required: installing plain `bumble` does not include gRPC/Netsim support.

## Start

1. Start an Android emulator with Bluetooth/Netsim support and enable Bluetooth in Android.
2. If Netsim is not enabled automatically, close the emulator and restart it with packet streaming enabled. With the Android emulator available on `PATH`:

   ```powershell
   emulator -list-avds
   emulator -avd <AVD_NAME> -packet-streamer-endpoint default
   ```

   Replace `<AVD_NAME>` with an AVD listed by the first command.
3. In another terminal, run:

   ```powershell
   .\.venv-bumble\Scripts\python.exe terminal\simulator\terminal_simulator.py
   ```

4. Build and launch the Android app, then open **Find nearby resources** from the Home screen. The app uses Android's Bluetooth APIs directly; no simulation flag is required.
5. Grant permissions, scan for `Locker 23`, select it, and connect. The expected ID is `550e8400-e29b-41d4-a716-446655440000`. Test **Open**, **Close**, and **Disconnect**.

The Python console logs connections, ID reads, command bytes, and status notifications. Press `Ctrl+C` to stop the peripheral. Advertising restarts after disconnection.

If Netsim discovery fails, inspect the generated `netsim.ini` configuration to identify its actual gRPC port, then pass `--transport android-netsim:localhost:PORT`. Do not use an arbitrary port. Emulator support depends on the installed emulator version and system image.

## Failure scenarios

Stop the simulator before starting another scenario, then reconnect in the app:

```powershell
.\.venv-bumble\Scripts\python.exe terminal\simulator\terminal_simulator.py --scenario door-open
```

| Scenario | Behavior |
|---|---|
| `success` (default) | Open releases the latch; Close locks it. The physical door remains closed. |
| `door-open` | The door sensor remains open, so Close returns result 3. |
| `denied` | Result 2; no movement. |
| `busy` | Result 4; no movement. |
| `actuator-error` | Result 5; no movement. |
| `timeout` | Sends pending status but no final result. |
| `disconnect` | Disconnects before reporting the result. |

Use `--delay 2` for slower responses. `--name`, `--id`, `--address`, and `--transport` are configurable; run `--help` for details. For multiple instances, use distinct BLE addresses and Netsim names.

## Tests

```powershell
.\.venv-bumble\Scripts\python.exe -m unittest discover -s terminal\simulator -v
```

These tests check binary formats, GATT service construction, and command logic with mocked notification delivery. They do not replace the app + Netsim integration test or a physical radio test. Successful emulation does not validate the final firmware or actuator.
