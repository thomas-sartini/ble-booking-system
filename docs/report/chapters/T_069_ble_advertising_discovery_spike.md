# Spike Investigation Report: BLE Advertising & Discovery 

- **Task:** T-069 — Research BLE advertising and discovery approach
- **Related Enabler:** EN-03.05 — Define BLE advertising and discovery protocol
- **Related Requirements:** FR-05.01, FR-05.05, NFR-01.03
- **Status:** Completed

---

## 1. Goal & Problem Statement

The objective of this spike is to investigate Bluetooth Low Energy (BLE) advertising and discovery mechanisms and recommend a practical, cross-platform compatible approach for detecting resource terminals (powered by the **Arduino Nano ESP32**) locally on Android and iOS mobile clients—**without requiring an active backend connection** during the discovery phase.

---

## 2. Recommended Strategy

We recommend a **Minimal Service-UUID Advertisement Strategy**:

1. **Terminal Advertising (Arduino Nano ESP32):** The terminal continuously broadcasts standard legacy BLE advertisements containing a single, custom **128-bit Service UUID** dedicated to the booking system.
2. **Mobile Scanning & Filtering (Android & iOS):** Both mobile platforms utilize OS-level hardware scan filters configured with this specific 128-bit Service UUID to efficiently isolate valid resource terminals from surrounding ambient BLE traffic.
3. **Deferred Metadata Exchange:** All rich terminal metadata (such as specific terminal/resource IDs, protocol version numbers, or operational status flags) is deferred until **after** a GATT connection is established (to be specified in **T-070**).
4. **Strict Separation of Discovery and Trust:** BLE advertisements are treated exclusively as an unauthenticated proximity and candidate-detection mechanism. Cryptographic identity verification happens strictly post-connection.

---

## 3. Alternatives Evaluated & Trade-offs

| Approach | Description | Pros | Cons / Risks | Verdict |
| :--- | :--- | :--- | :--- | :--- |
| **Option A: Minimal Service-UUID Only** | Primary advertisement packet contains only Flags + 128-bit Service UUID. | Fits comfortably within the 31-byte legacy BLE limit; enables native OS background scanning on iOS & Android; minimal battery impact. | Cannot distinguish between two adjacent terminals prior to connection (must rely on RSSI or post-connect GATT read). | **Recommended** |
| **Option B: Rich Payload in Advertisement** | Embed Terminal ID, protocol version, and status flags into Manufacturer Data alongside the UUID. | Mobile app immediately knows which terminal/room is advertising without connecting. | Exceeds or tightly crowds the 31-byte legacy packet limit; breaks or complicates iOS background filtering rules; leaks static identifiers publicly. | Rejected |
| **Option C: Scan Response Metadata** | Keep primary packet minimal, place Terminal ID/version in the active Scan Response packet. | Keeps primary advertisement clean while allowing active scanners to fetch extra metadata before connecting. | Requires active scanning (higher power consumption); Scan Response delivery is not guaranteed in congested RF environments or iOS background modes. | Optional fallback only |

---

## 4. Technical Details & Platform Constraints

### 4.1 Legacy 31-Byte Payload Limit
Standard BLE 4.x/5.0 legacy advertising packets are strictly capped at **31 bytes**. Using BLE 5.0 Extended Advertising is avoided to guarantee maximum interoperability across all target smartphones and the Arduino Nano ESP32 BLE stack.

- **Flags (`AD Type 0x01`):** 3 bytes (`LE General Discoverable Mode`, `BR/EDR Not Supported`)
- **Complete List of 128-bit Service UUIDs (`AD Type 0x07`):** 18 bytes (2-byte header + 16-byte UUID)
- **Total Used:** **21 bytes** (leaving 10 bytes headroom, e.g., for an optional short local name or kept empty for efficiency).

### 4.2 Mobile Platform Support & Limitations

- **Android (`BluetoothLeScanner`):**
  * Supports hardware-level filtering via `ScanFilter.Builder().setServiceUuid(ParcelUuid)`.
  * Requires runtime permissions (`BLUETOOTH_SCAN`, and `ACCESS_FINE_LOCATION` on older API levels).
  * Background scanning is throttled by Android OS unless using a `PendingIntent` scan or foreground service, making hardware UUID filtering essential for battery efficiency.
- **iOS (`CoreBluetooth`):**
  * Supports hardware-level filtering via `CBCentralManager.scanForPeripherals(withServices: [CBUUID])`.
  * **Critical iOS Constraint:** For iOS to discover peripherals while the app is suspended or in the background, the 128-bit Service UUID **must** be explicitly included in the primary advertisement packet's Service UUID array. Placing custom identifiers solely in Manufacturer Data will prevent iOS background discovery.

### 4.3 Arduino Nano ESP32 Capabilities
- The ESP32 BLE stack (NimBLE / Bluedroid via Arduino Core) natively supports broadcasting custom 128-bit Service UUIDs in primary advertising packets.
- Recommended advertising interval: **100 ms – 250 ms** to balance fast user check-in responsiveness with reasonable power usage.

---

## 5. Security & Privacy Boundaries (`NFR-01.03`)

1. **Advertisements Are Not Proof of Authenticity:**
   * BLE advertising packets are broadcast in plaintext over the air and can be sniffed or cloned (spoofed) by anyone with standard BLE hardware.
   * **Rule:** Discovering a matching Service UUID only signals that a device *claims* to be a system terminal candidate. It **must never** be trusted as proof of terminal identity or used to grant access.
2. **Cryptographic Verification Happens Post-Connection:**
   * Once the mobile app initiates a GATT connection to a discovered candidate, the terminal and mobile app must perform a cryptographic handshake / challenge-response verification (to be detailed in **T-070**) before exchanging sensitive access tickets.
3. **Privacy Considerations:**
   * Avoiding static terminal identifiers or user data in the public broadcast reduces passive tracking and profiling of infrastructure details by unauthorized third-party scanners.

---

## 6. Open Questions & Implementation Risks

- **Multi-Terminal Disambiguation (Dense Rooms):** In corridors with multiple adjacent rooms, scanning for a shared Service UUID will return multiple candidates simultaneously.
  * *Mitigation:* Sort discovered candidates by signal strength (RSSI) to prioritize the closest terminal, and verify the exact `terminal_id` / `resource_id` immediately upon GATT connection.
- **Scan Latency vs. Battery Drain:** Continuous aggressive scanning drains mobile batteries.
  * *Mitigation:* Trigger active/balanced BLE scanning primarily when the user opens the check-in screen or actively initiates an access flow.
- **OS Permission Handling:** Users denying Bluetooth or Location permissions on mobile devices will block discovery.
  * *Mitigation:* Provide clear UI onboarding prompts explaining why local BLE access is required for terminal check-in.

---

## 7. Next Steps (Transition to `T-070`)

With the discovery mechanism established, **T-070** will define the post-connection GATT protocol specification, including:
- Custom GATT Service and Characteristic UUIDs.
- Message framing, versioning, and error codes.
- Cryptographic authentication handshake and offline access ticket exchange.