package ch.hslu.blebooking.mobile.ui.navigation

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import ch.hslu.blebooking.mobile.ble.model.DiscoveredTerminal

/** Carries a discovered terminal using arguments that also survive process recreation. */
object TerminalRoute {
    private const val ADDRESS = "terminalAddress"
    private const val NAME = "terminalName"
    private const val RSSI = "terminalRssi"

    val pattern = "${AppScreens.TERMINAL.name}/{$ADDRESS}/{$RSSI}?$NAME={$NAME}"
    val arguments = listOf(
        navArgument(ADDRESS) { type = NavType.StringType },
        navArgument(RSSI) { type = NavType.IntType },
        navArgument(NAME) {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        },
    )

    fun create(terminal: DiscoveredTerminal): String {
        val route = "${AppScreens.TERMINAL.name}/${Uri.encode(terminal.address)}/${terminal.rssi}"
        return terminal.name?.let { "$route?$NAME=${Uri.encode(it)}" } ?: route
    }


    fun read(state: SavedStateHandle): DiscoveredTerminal = DiscoveredTerminal(
        name = state[NAME],
        address = checkNotNull(state.get<String>(ADDRESS)),
        rssi = checkNotNull(state.get<Int>(RSSI)),
    )
}
