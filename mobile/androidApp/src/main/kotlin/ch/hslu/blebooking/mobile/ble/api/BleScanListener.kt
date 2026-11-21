package ch.hslu.blebooking.mobile.ble.api

import ch.hslu.blebooking.mobile.ble.model.DiscoveredTerminal

interface BleScanListener {
    /** Receives the Android scan failure code so callers can stop loading and offer a retry. */
    fun onScanFailed(errorCode: Int) {}

    /**
     * Receives scan observations; the same candidate may be reported more than once.
     *
     * @param terminal Candidate observed in this scan result, not yet identified or authenticated.
     */
    fun onTerminalDiscovered(terminal: DiscoveredTerminal){

    }
}
