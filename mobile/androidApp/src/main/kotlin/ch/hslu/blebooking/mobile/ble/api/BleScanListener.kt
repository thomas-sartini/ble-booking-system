package ch.hslu.blebooking.mobile.ble.api

import ch.hslu.blebooking.mobile.ble.model.DiscoveredTerminal

interface BleScanListener {
    /**
     * Receives scan observations; the same candidate may be reported more than once.
     *
     * @param terminal Candidate observed in this scan result, not yet identified or authenticated.
     */
    fun onTerminalDiscovered(terminal: DiscoveredTerminal){

    }
}
