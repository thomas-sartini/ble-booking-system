# Locker prototype: shopping list

**T-015 · 14 October 2026 · Budget: CHF 200**

Parts for the [Nano ESP32 and servo prototype](terminal-prototype-hardware.md). The powerbank, a USB-A-to-C cable for the Nano, computers, phones, tools and printer access are already available and excluded from purchases. The special 5 V power cable, wiring and used printing material are included.

## Parts to buy

Prices include VAT and exclude delivery. They are planning values from the linked suppliers.

| Qty | Part and supplier | Unit CHF | Line CHF | Use / note |
|---:|---|---:|---:|---|
| 1 | [Arduino Nano ESP32 with headers, ABX00083](https://www.galaxus.ch/de/s1/product/arduino-nano-esp32-with-headers-entwicklungsboard-kit-37112615) — Galaxus | 19.50 | 19.50 | Controller; soldered headers. |
| 1 | [MG90S positional micro servo with metal gears](https://www.bastelgarage.ch/towerpro-micro-servo-mg90s-mit-metallgetriebe) — Bastelgarage | 7.90 | 7.90 | Position-controlled version; horn and screws included. |
| 1 | [Adafruit DS3231 RTC breakout, 3013](https://www.play-zone.ch/de/adafruit-ds3231-precision-rtc-breakout.html) — Play-Zone | 24.90 | 24.90 | 3.3 V clock; supplied header needs soldering. |
| 1 | [CR1220 3 V lithium cell](https://www.bastelgarage.ch/cr1220-knopfzellen-lithium-batterie-3v) — Bastelgarage | 1.20 | 1.20 | Backup battery for this RTC module. |
| 1 | [Magnetic door contact with magnet](https://www.bastelgarage.ch/magnetischer-tur-fenster-kontakt) — Bastelgarage | 4.90 | 4.90 | Door-open / door-closed input. |
| 1 | [Adafruit USB-C PD to DC cable, 5968 — 5 V](https://www.berrybase.ch/adafruit-usb-c-pd-zu-5-5mm-dc-barrel-jack-kabel-1-2m-emark-chip-5a-5v) — BerryBase | 7.20 | 7.20 | USB-C plug connects to the existing powerbank; fixed 5 V output. |
| 1 | [Female DC jack to screw terminal, P00002831](https://www.play-zone.ch/de/dc-hohlbuchse-5-5-2-1-mm-weiblich-lotfreier-netzteilanschluss.html) — Play-Zone | 1.60 | 1.60 | Connects the 5 V cable to the distribution leads. |
| 1 | [Soldered USB-A female breakout, SOL-333132](https://www.berrybase.ch/soldered-usb-typ-a-buchse-breakout-board-tht-pins) — BerryBase | 1.45 | 1.45 | 5 V power outlet for the Nano's existing USB-A-to-C cable; solder two power leads. |
| 1 | [1000 µF / 25 V electrolytic capacitor](https://www.bastelgarage.ch/elektrolyt-kondensator-1000-f-25-v) — Bastelgarage | 0.95 | 0.95 | Across servo power near its connector; observe polarity. |
| 1 | [Half-size breadboard](https://www.bastelgarage.ch/breadboard-lochraster-steckplatine-half-size) — Bastelgarage | 4.90 | 4.90 | Nano and low-current signal connections. |
| 1 | [20 male–male jumper wires, 20 cm](https://www.bastelgarage.ch/dupont-kabel-m-m-20cm-20-stuck) — Bastelgarage | 2.90 | 2.90 | Signal wiring. |
| 1 | [20 male–female jumper wires, 20 cm](https://www.bastelgarage.ch/dupont-kabel-m-f-20cm-20-stuck) — Bastelgarage | 2.90 | 2.90 | Clock and module wiring. |
| 6 | [PCT-213 three-way lever connector](https://www.bastelgarage.ch/pct-213-3pin-federklemme-betatigungshebel) — Bastelgarage | 0.90 | 5.40 | 5 V and ground distribution, including linked connectors where needed. |
| | **Parts subtotal** | | **85.70** | |

## Material, delivery and reserve

These are allowances rather than product quotations.

| Item | CHF | Basis |
|---|---:|---|
| Servo adapter lead, short power wires and insulation | 10.00 | Prepare a separate servo power branch and a signal connection. |
| Screws, nuts and mounting material | 5.00 | Sizes follow the printed mounts. |
| PLA for model and test pieces | 15.00 | Material use through the available printer. |
| Delivery | 40.00 | Allowance across four suppliers; BerryBase lists CHF 9.50, remaining charges depend on checkout. |
| Reserve | 15.00 | For price changes or small assembly parts. |
| **Allowances subtotal** | **85.00** | |

[BerryBase delivery](https://www.berrybase.ch/versand-und-zahlungsbedingungen/) is listed at CHF 9.50 below CHF 200. The total delivery allowance includes the other suppliers.

## Budget

| | CHF |
|---|---:|
| Listed parts | 85.70 |
| Material, delivery and reserve | 85.00 |
| **Planned total** | **170.70** |
| **Remaining from CHF 200** | **29.30** |

The optional [0.96-inch I2C OLED](https://www.bastelgarage.ch/oled-display-weiss-i2c-128x64-0-96) adds **CHF 10.90**, giving a total of **CHF 181.60** and **CHF 18.40** remaining.

## Connection and assembly

1. Connect the **5 V version, Adafruit 5968**, to the powerbank USB-C output and the female DC adapter. Measure its output before connecting the electronics.
2. Split 5 V and ground into two branches. One goes directly to the servo adapter lead; the other goes to VUSB and GND on the USB-A socket board. Solder the socket's two power leads, leave its data pins unused, and connect the Nano with the existing USB-A-to-C cable.
3. Join Nano ground and servo ground. The servo's red lead receives 5 V, brown is ground, and orange is the control signal. Its power branch bypasses the Nano and breadboard. Put the capacitor across 5 V and ground near the servo, observing polarity.
4. Supply the RTC and optional OLED from Nano 3V3 and use I2C. Solder the RTC header. Connect the door contact to a GPIO input with a 3.3 V pull-up.
5. Test servo movement without a latch first. Verify response to the 3.3 V control signal and fit a suitable level shifter if the purchased servo requires it. Calibrate the movement range without driving into the servo's internal limits or the printed stop.

The Nano receives 5 V through **USB-C**, not VIN or 3V3. For programming, disconnect its power-socket cable and connect USB directly to the computer. A stationary regulated 5 V supply can replace the powerbank at the same distribution input.

Print the latch test and mounting pieces after measuring the parts. The complete enclosure follows once the latch moves freely. Final prices, stock and delivery are confirmed before ordering.
