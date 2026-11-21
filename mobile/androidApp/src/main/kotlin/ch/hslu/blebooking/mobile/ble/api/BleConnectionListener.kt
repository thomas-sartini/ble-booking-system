package ch.hslu.blebooking.mobile.ble.api

import ch.hslu.blebooking.mobile.ble.gatt.BleGattSession

interface BleConnectionListener {
    fun onReady(session: BleGattSession)
    fun onDisconnected()
    fun onConnectionError(message: String)

}