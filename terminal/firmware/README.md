# Terminal demo firmware (Arduino Nano ESP32)

This is the «Lending Station 01» firmware used at the HSLU Bachelor-Info-Tag Informatik on 21 November 2026 in Zug-Rotkreuz.

| Folder | What it is |
|---|---|
| `lending_station_demo/` | Info Day demo sketch. Combines the T6 loan flow with Bluetooth according to `docs/ble-demo-protocol.md` (PR #144). Formerly T9. |
| `tests/T1_LED/` | RGB LED of the Nano (seen through the light pipe) |
| `tests/T2_Summer/` | Active buzzer on D3 |
| `tests/T3_Tuerkontakt/` | Door contact on D2 |
| `tests/T4_Uhr_RTC/` | Clock DS3231 (I²C) |
| `tests/T5_Servo_Kalibrierung/` | Servo on D9: set the centre, find the end positions of the latch |
| `tests/T6_Ausleihbox_Ablauf/` | Complete loan flow without Bluetooth, controlled from the Serial Monitor |
| `tests/T7_BLE_Advertising/` | Bluetooth advertising only |
| `tests/T8_BLE_Demo_Simulation/` | Protocol simulator on a bare Nano, like `terminal/simulator`: no latch, door contact, LED or buzzer. Useful for app tests without the box. |

Comments and Serial Monitor messages are in German; they follow the build handbook of the box.

## Setup

- Arduino IDE 2, board package «Arduino ESP32 Boards» 2.0.18-arduino.5, board «Arduino Nano ESP32».
- Tools → Pin Numbering → «By GPIO number (legacy)» for T5, T6 and `lending_station_demo` (needed by ESP32Servo; otherwise compiling stops with an error message).
- Libraries: ESP32Servo 3.2.1, RTClib 2.1.4 (with Adafruit BusIO 1.17.4). BLE and NVS come with the board package.
- Serial Monitor: 115200 baud, line ending «New Line».

## Order

1. T1 to T5 while building the box. T5 gives the two servo angles.
2. Enter `LOCK_ANGLE` and `RELEASE_ANGLE` in T6 and `lending_station_demo`. Box 01 uses 100 and 10.
3. T6 to test the whole flow without a phone (`a` borrow, `z` return, `s` status, `h` help).
4. Open `lending_station_demo/lending_station_demo.ino` in the Arduino IDE for the demo with the app. The Serial Monitor commands of T6 keep working.

## Bluetooth (`lending_station_demo`, T7, T8)

- Name «Lending Station 01», Booking Service `fd698d10-c5c7-429b-9fb3-18a41dd73998`, advertising every 100 ms at −24 dBm. The app limits the distance by the signal strength (RSSI).
- GATT as in `docs/ble-demo-protocol.md`:
  - Terminal Information (read): `01` + terminal ID (example ID of the protocol)
  - Door Command (write): `[01][request ID, 2 bytes LE][01 OPEN / 02 CLOSE]`
  - Door Status (read, notify): `[01][request ID][command][result][latch][door]`
- No pairing and no encryption: demo only, securing follows with T-003.

### Results sent by `lending_station_demo`

Right after a command the box notifies `00` (running), then one of these:

| Code | Name | Meaning |
|---|---|---|
| `01` | SUCCESS | OPEN: loan saved, latch released. CLOSE: return saved, latch locked. |
| `03` | DOOR_NOT_CLOSED | CLOSE while the door is open or closed for less than 500 ms |
| `04` | BUSY | A command arrived while another one was running |
| `06` | INVALID_COMMAND | Command byte is neither `01` nor `02` |
| `07` | ALREADY_OPEN | OPEN, but a loan is already active |
| `08` | ALREADY_LOCKED | CLOSE, but there is no loan and the latch is locked |
| `09` | STORAGE_NOT_READY | Storage not initialised or unreadable; everything is blocked |
| `0A` | STORAGE_WRITE_FAILED | Saving or reading back failed; nothing moved |
| `0B` | DOOR_OPENED_DURING_LOCK | Return saved, but the door opened while locking; the box locks by itself once the door is closed |
| `0C` | RETURN_PENDING | Return already saved, the box waits for the door to close |

`02` stays reserved for the authorisation (T-003). `05` is not sent: the servo has no position feedback.
Latch: `00` unknown, `01` locked, `02` released. Door: `01` closed (for 500 ms), `02` open.

Codes `07` to `0C` go beyond the demo protocol. The Android demo app (PR #144) accepts results `0` to `6` only; it needs `decodeDoorStatus` to accept `0` to `12` and a message for each new code.
