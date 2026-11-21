package ch.hslu.blebooking.mobile.ble.model

/**
 * A candidate observed during BLE discovery.
 *
 * @property name Optional advertised display label; not a unique or trusted identifier.
 * @property address BLE device address; not the Terminal ID read through GATT.
 * @property rssi Received signal strength in dBm; not a direct distance measurement.
 */
data class DiscoveredTerminal(
    val name: String?,
    val address: String,
    val rssi: Int
){
}
