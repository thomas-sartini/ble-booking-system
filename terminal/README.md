# Terminal

The terminal hardware concept is the Ausleihbox 01 equipment lending station. It
uses an Arduino Nano ESP32, an MG90S servo-driven latch, a magnetic door contact,
a DS3231 real-time clock and an active buzzer. The enclosure combines 8 mm plywood
with 3D-printed mechanical parts and mounts.

## Interactive assembly and wiring guide

**[Open the interactive 3D model](https://thomas-sartini.github.io/ble-booking-system/3D-Modell_Ausleihbox.html)**

The guide includes:

- 37 illustrated and animated assembly steps.
- Nine wiring steps alongside the corresponding assembly stages.
- Component selection, measurements, predefined views and an exploded view.
- A simulation of the borrowing and return sequence.

The interface and assembly instructions are in German. The online link becomes
available after GitHub Pages is enabled and the first deployment completes.

The self-contained HTML source is
[`3d-model/3D-Modell_Ausleihbox.html`](3d-model/3D-Modell_Ausleihbox.html).

## GitHub Pages

A repository administrator must select **GitHub Actions** under **Settings > Pages**.
After the changes are merged into `main`, the `ausleihbox-pages.yml` workflow
publishes the model. A manual run on `main` is also supported.

The workflow creates the required `index.html` automatically in its deployment
output. It is not stored alongside the model in the repository. The source HTML
retains its filename and is also available at the direct link above.

To update the model, replace `3d-model/3D-Modell_Ausleihbox.html`, check its
JavaScript syntax and follow the repository's branch and pull-request process.

## Related work

- [T-045: Assemble terminal hardware prototype](https://github.com/thomas-sartini/ble-booking-system/issues/83)
  covers physical assembly and relevant mechanical or assembly changes.
- [T-046: Validate terminal hardware interfaces](https://github.com/thomas-sartini/ble-booking-system/issues/84)
  covers on-device checks of power, GPIO, I2C and the connected peripherals.

The build guide supports this work; the simulation does not establish completion
of the physical assembly or hardware tests.
