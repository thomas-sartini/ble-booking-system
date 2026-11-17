package ch.hslu.blebooking.mobile.ble.gatt

/** Hardware calls whose asynchronous results arrive through the session's event channel. */
internal interface BleGattTransport {
    fun readTerminalInformation(): Boolean
    fun writeDoorCommand(payload: ByteArray): Boolean
    fun enableDoorStatusNotifications(): Boolean
}
