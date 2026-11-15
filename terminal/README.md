# Terminal

The Ausleihbox 01 uses an Arduino Nano ESP32, an MG90S servo latch, a magnetic door
contact, a DS3231 RTC and an active buzzer. The enclosure is made from 8 mm plywood
and 3D-printed parts.

## 3D model

[Open the 3D model](https://thomas-sartini.github.io/ble-booking-system/3D-Modell_Ausleihbox.html)

The model contains 37 assembly steps, nine wiring steps, measurements, an exploded
view and a borrowing simulation. The interface is in German.

Source: [`3d-model/3D-Modell_Ausleihbox.html`](3d-model/3D-Modell_Ausleihbox.html).

## Deployment

The `ausleihbox-pages.yml` workflow publishes the model from `main` to GitHub Pages.
Initial setup: **Settings > Pages > GitHub Actions**. The workflow generates the
start page during deployment.

## Tasks

- [T-045: Hardware assembly](https://github.com/thomas-sartini/ble-booking-system/issues/83)
- [T-046: Hardware interface tests](https://github.com/thomas-sartini/ble-booking-system/issues/84)
