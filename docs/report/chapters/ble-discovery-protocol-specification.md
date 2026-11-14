# Protocol Specification: BLE Advertising & Discovery 

- **Status:** Ready for Implementation
- **Related Enabler:** Define BLE advertising and discovery protocol
- **Related Requirements:** Terminal discovery, terminal/mobile communication contract, communication security boundary

---

## 1. Scope & Objective

This document defines the binding technical protocol specification for Bluetooth Low Energy (BLE) advertising and candidate discovery across the resource terminals (Arduino Nano ESP32) and mobile client applications (Android and iOS). It translates the underlying research findings into precise implementation rules for firmware and mobile developers.

---

## 2. BLE Service UUID Definition

To isolate system-related terminals from ambient Bluetooth traffic, a dedicated global 128-bit Service UUID is used.

- **Assigned 128-bit Service UUID:** 
  `b4e78a20-058c-4f1a-93e1-3827a51c9d2f`
- **Code Maintenance Location:**
  * **Firmware (Arduino Nano ESP32):** Defined as a global constant in `ble_config.h` (or main firmware initialization module).
  * **Mobile Apps (Android/iOS):** Defined as a shared constant in the mobile networking/BLE configuration layer (e.g., `BLEConstants.kt` / `BLEConstants.swift`).

---

## 3. Advertising Payload Specification (Terminal Side)

Resource terminals shall broadcast standard legacy BLE advertising packets adhering strictly to the **31-byte legacy limit** to ensure full cross-platform compatibility and reliable background scanning.

### 3.1 Packet Layout
* **Flags (`AD Type 0x01`):** `0x06` (LE General Discoverable Mode, BR/EDR Not Supported) — **3 bytes**
* **Complete List of 128-bit Service UUIDs (`AD Type 0x07`):** `b4e78a20-058c-4f1a-93e1-3827a51c9d2f` (encoded in little-endian format as required by the BLE specification) — **18 bytes**
* **Headroom:** ~10 bytes remaining (left blank or used for standard manufacturer flags if required).

### 3.2 Exclusions from Advertisement
* **No Terminal/Resource IDs** and **No Protocol Version Numbers** are included in the primary advertisement packet. 
* *Rationale:* Packing custom identifiers breaks iOS background discovery filters and exceeds legacy limits. Versioning and terminal identification are deferred to the post-connection GATT phase.

### 3.3 Advertising Parameters
* **Interval:** 100 ms to 250 ms (balancing fast user discovery with optimal power consumption).
* **Tx Power:** Standard calibrated transmission power to limit over-the-air bleed into adjacent physical rooms.

---

## 4. Scanning & Filtering Behavior (Mobile Clients)

Both Android and iOS applications implement identical high-efficiency scan filters to detect nearby terminal candidates.

### 4.1 Android Implementation (`BluetoothLeScanner`)
* **Scan Filter:** Must configure a `ScanFilter` matching the exact 128-bit Service UUID (`b4e78a20-058c-4f1a-93e1-3827a51c9d2f`).
* **Scan Settings:** Mode set to `SCAN_MODE_BALANCED` or `SCAN_MODE_LOW_LATENCY` when the user actively opens the check-in interface.

### 4.2 iOS Implementation (`CoreBluetooth`)
* **Scan Options:** `CBCentralManager.scanForPeripherals(withServices: [CBUUID(string: "b4e78a20-058c-4f1a-93e1-3827a51c9d2f")], options: nil)`.
* **Background Requirement:** Because the 128-bit Service UUID is present in the primary advertisement packet's service array, iOS background scanning will successfully wake/notify the app when a terminal is in proximity.

---

## 5. Handling Non-Target and Invalid Advertising Data

Mobile clients shall handle incoming BLE advertisements according to the following strict filtering logic:
1. **Unrelated Devices:** Any peripheral lacking the exact 128-bit Service UUID in its advertising packet is silently ignored by the OS hardware filter.
2. **Malformed Packets / Invalid Formats:** If a packet matches the Service UUID but contains corrupted payload data or malformed flags, the mobile client drops the candidate to prevent parsing errors.
3. **Multi-Terminal Disambiguation:** If multiple valid terminals are detected simultaneously (e.g., in adjacent rooms), the mobile application sorts candidates locally by **RSSI (Signal Strength)** in descending order, presenting the closest physical terminal as the primary candidate.

---

## 6. Security Boundaries & Authentication Separation

Discovery is strictly decoupled from trust:
* **No Authentication in Discovery:** A matching advertisement packet or Bluetooth MAC address only indicates the *presence* of a candidate device. It **must never** be interpreted as proof of terminal identity or authorization.
* **Transition to GATT:** Discovering a candidate triggers an immediate request to establish a secure GATT connection. All cryptographic handshakes, version checks, and ticket validations occur exclusively *after* connection establishment over protected GATT characteristics.