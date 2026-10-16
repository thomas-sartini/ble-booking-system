# Equipment lending station: shopping list

**T-015 · 14 October 2026, updated 17 October 2026 · Budget: CHF 200**

*Prices and stock checked: 16 October 2026.*

This list covers the purchases for our [Nano ESP32 lending station prototype](terminal-prototype-hardware.md). Prices include Swiss VAT. We plan three orders: electronics, wiring and cable mounts from Bastelgarage; the Nano from Galaxus; and plywood, wood glue and cutting from HORNBACH Luzern.

## 1. Bastelgarage order

| Qty | Part and article number | Unit CHF | Line CHF | Purpose / assembly |
|---:|---|---:|---:|---|
| 1 | [MG90S positional micro servo with metal gears, 420309](https://www.bastelgarage.ch/towerpro-micro-servo-mg90s-mit-metallgetriebe) | 7.90 | 7.90 | Positional servo for the printed latch; horn and screws included. |
| 1 | [DS3231 RTC module with AT24C32 EEPROM, 420105](https://www.bastelgarage.ch/rtc-ds3231-zeitmodul-mit-at24c32-eeprom-speicher-32k) | 8.90 | 8.90 | 3.3 V I2C clock, address 0x68; fitted header; 38.5 x 22 x 13 mm. |
| 1 | [LIR2032 rechargeable backup cell, 3.6 V / 40 mAh, 420113](https://www.bastelgarage.ch/knopfzellen-akku-batterie-lir2032-lithium-3-6v) | 4.50 | 4.50 | Replaceable RTC backup cell; charged before first use. |
| 1 | [LIR2032 USB-C charger for coin cells, 423446](https://www.bastelgarage.ch/lir2032-usb-c-ladegerat-fur-knopfzellen) | 7.90 | 7.90 | Charges the LIR2032 before first use; stays outside the station. |
| 1 | [Magnetic door contact with magnet, 420310](https://www.bastelgarage.ch/magnetischer-tur-fenster-kontakt) | 4.90 | 4.90 | Door-open / door-closed input; contact and magnet. |
| 1 | [Waveshare regulated 5 V / 4 A plug-in power supply, 17679 (423817)](https://www.bastelgarage.ch/bauteile/netzteile-strom/stecknetzteil/5v-dc-4000ma-stecknetzteil-5-5mm-2-1mm-stecker) | 15.90 | 15.90 | 5 V / 4 A; 5.5 / 2.1 mm DC output; EU plug version. |
| 1 | [Female 5.5 / 2.1 mm DC jack with screw terminals, 420128](https://www.bastelgarage.ch/dc-buchse-female-barrel-jack-5-5mm-2-1mm-mit-schraubklemmen) | 1.90 | 1.90 | 36.5 x 14 x 13 mm; rear power inlet in a printed holder. |
| 1 | [DFRobot USB-C male to screw-terminal cable, FIT0993 (424303)](https://www.bastelgarage.ch/5-pin-typ-c-stecker-auf-schraubklemme-30cm-5v-2a) | 9.90 | 9.90 | 30 cm, 5 V / 2 A; screw-terminal power connection to Nano USB-C; no soldering. |
| 1 | [1000 µF / 25 V electrolytic capacitor, 420416](https://www.bastelgarage.ch/elektrolyt-kondensator-1000-f-25-v) | 0.95 | 0.95 | Capacitor across the servo supply. |
| 1 | [SYB-170 mini breadboard, 170 contacts, 420365](https://www.bastelgarage.ch/syb-170-mini-breadboard-170-pins-weiss) | 2.90 | 2.90 | 47 x 35 x 9 mm; Nano mounting and signal connections. |
| 1 | [20 male–male jumper wires, 20 cm, 420619](https://www.bastelgarage.ch/dupont-kabel-m-m-20cm-20-stuck) | 2.90 | 2.90 | Signal connections on the breadboard; servo plug to the lever connectors. |
| 1 | [20 male–female jumper wires, 20 cm, 420620](https://www.bastelgarage.ch/zubehor/dupont-kabel-m-f-20cm-20-stuck) | 2.90 | 2.90 | Connections to the separately mounted modules and buzzer. |
| 6 | [PCT-213 three-way lever connector, 420797](https://www.bastelgarage.ch/pct-213-3pin-federklemme-betatigungshebel) | 0.90 | 5.40 | 5 V and ground distribution outside the breadboard. |
| 1 | [Active piezo buzzer, 420248](https://www.bastelgarage.ch/piezo-buzzer-summer-aktiv) | 1.50 | 1.50 | 3.3-5 V, 15-20 mA; diameter 12 mm; separate printed holder. |
| 1 | [Stranded wire 5 x 0.75 mm², 1 m, 425234](https://www.bastelgarage.ch/litze-5-x-0-75mm-18awg-weiss-blau-rot-grun-schwarz) | 3.50 | 3.50 | Red and black cores for 5 V and ground between DC jack, lever connectors and USB-C terminal. |
| 1 | [Black nylon cable ties, 3 x 100 mm, pack of 100, 420646](https://www.bastelgarage.ch/mechanische-bauteile/kabelbinder/schwarze-3x100mm-nylon-kabelbinder-100-stuck) | 3.90 | 3.90 | Bundle and secure the internal wiring away from the latch. |
| 4 | [Self-adhesive cable-tie mounts, white, 20 x 20 mm, 421476](https://www.bastelgarage.ch/mechanische-bauteile/kabelbinder/20x20mm-kabelbindersockel-weiss-selbstklebend) | 0.20 | 0.80 | Attachment points for the wiring on the component mounting plate. |
| | **Bastelgarage total** | | **86.55** | |

## 2. Galaxus order

| Qty | Part | Unit CHF | Line CHF | Purpose / assembly |
|---:|---|---:|---:|---|
| 1 | [Arduino Nano ESP32 with headers, ABX00083](https://www.galaxus.ch/de/s1/product/arduino-nano-esp32-with-headers-entwicklungsboard-kit-37112615) | 19.50 | 19.50 | 45 x 18 mm; headers already fitted. |
| | **Galaxus total** | | **19.50** | |

## 3. HORNBACH Luzern order

We have estimated the cutting cost at CHF 1 per cut. The final price and whether the store can cut the small panels still need to be confirmed. The 41 mm-wide cover may need to be cut separately in a workshop.

| Qty | Part and article number | Unit CHF | Line CHF | Purpose / assembly |
|---:|---|---:|---:|---|
| 1 | [Poplar plywood, raw, 6 mm, 600 x 600 mm, 10053590](https://www.hornbach.ch/de/p/sperrholzplatte-pappel-roh-b-bb-6-mm-600x600-mm/10053590/) | 8.65 | 8.65 | Eight panels: back, sides, top, bottom, divider, door and electronics cover. |
| 1 | [Ponal Classic wood glue, 120 g, 2540321](https://www.hornbach.ch/de/p/ponal-classic-holzleim-weissleim-120-g/2540321/) | 3.95 | 3.95 | Joining the wooden body panels and divider. |
| 13 | [In-store cutting service](https://www.hornbach.ch/de/conf/holzzuschnitt-nach-mass/80000/), per cut | 1.00 | 13.00 | Planning estimate for 13 straight cuts at CHF 1.00 each; supplier lists prices from CHF 0.20. |
| | **HORNBACH total** | | **25.60** | |

### Cutting cost estimate

The [cutting plan in section 5](terminal-prototype-hardware.md#plywood-cutting-list) needs three strip cuts, eight crosscuts and two height trims: 13 straight cuts in total. The latch cutout and holes for fittings, LED, buzzer and rear power entry are made during assembly and are not included in this estimate.

## Budget

| Order | CHF |
|---|---:|
| Bastelgarage | 86.55 |
| Galaxus | 19.50 |
| HORNBACH Luzern | 25.60 |
| **Total** | **131.65** |
| **Remaining from CHF 200** | **68.35** |

The total includes the listed parts and estimated cutting. The 3D printer, filament, assembly screws and nuts are privately available.

Bastelgarage offers [free delivery from CHF 80](https://www.bastelgarage.ch/lieferung-versandkosten); we collect the Galaxus order in Wohlen and the HORNBACH order in Luzern.

## Availability

| Supplier | Stock on 16 October 2026 |
|---|---|
| Bastelgarage | All 17 items listed as in stock. |
| Galaxus | More than ten Nano boards listed in stock. |
| HORNBACH Luzern | 30 plywood panels listed in the Luzern store. |

These are dated supplier indications; no stock is reserved.
