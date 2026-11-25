package ch.hslu.blebooking.mobile.ui.navigation

import android.net.Uri
import ch.hslu.blebooking.mobile.ui.screens.scanning.ScanningView
import ch.hslu.blebooking.mobile.ui.screens.scanning.ScanningViewModel
import ch.hslu.blebooking.mobile.ui.screens.terminal.TerminalView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ch.hslu.blebooking.mobile.ui.components.MessageData
import ch.hslu.blebooking.mobile.ui.components.MessageType
import ch.hslu.blebooking.mobile.ui.screens.home.HomeScreen
import ch.hslu.blebooking.mobile.ui.screens.bookings.BookingView
import ch.hslu.blebooking.mobile.ui.screens.login.LoginScreen

enum class AppScreens { LOGIN, HOME, BOOKINGS, SCANNING, TERMINAL }

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = AppScreens.LOGIN.name,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(AppScreens.SCANNING.name) {
            val scanner: ScanningViewModel = hiltViewModel()
            ScanningView(
                onTerminalSelected = { terminal ->
                    val route = "${AppScreens.TERMINAL.name}/${Uri.encode(terminal.address)}?rssi=${terminal.rssi}" +
                        (terminal.name?.let { "&name=${Uri.encode(it)}" } ?: "")
                    navController.navigate(route) { launchSingleTop = true }
                },
                onBack = { navController.popBackStack() }, viewModel = scanner,
            )
        }
        composable(
            route = "${AppScreens.TERMINAL.name}/{address}?rssi={rssi}&name={name}",
            arguments = listOf(
                navArgument("address") { type = NavType.StringType },
                navArgument("rssi") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) {
            TerminalView(onBack = { navController.popBackStack() })
        }
        composable(AppScreens.BOOKINGS.name) { entry ->
            val homeEntry = remember(entry) {
                navController.getBackStackEntry("${AppScreens.HOME.name}?message={message}&type={type}")
            }
            BookingView(
                onBack = { navController.popBackStack() },
                viewModel = hiltViewModel(homeEntry),
            )
        }
        composable(
            route = "${AppScreens.LOGIN.name}?message={message}&type={type}",
            arguments = listOf(
                navArgument("message") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = MessageType.Success.name
                },
            ),
        ) { navBackStackEntry ->
            val text = navBackStackEntry.arguments?.getString("message")
            val type = MessageType.entries.firstOrNull {
                it.name == navBackStackEntry.arguments?.getString("type")
            } ?: MessageType.Success
            val messageData = text?.let { MessageData(text = it, type = type) }

            LoginScreen(navController = navController, messageData = messageData)
        }
        composable(
            route = "${AppScreens.HOME.name}?message={message}&type={type}",
            arguments = listOf(
                navArgument("message") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = MessageType.Success.name
                },
            ),
        ) { navBackStackEntry ->
            val text = navBackStackEntry.arguments?.getString("message")
            val type = MessageType.entries.firstOrNull {
                it.name == navBackStackEntry.arguments?.getString("type")
            } ?: MessageType.Success
            val messageData = text?.let { MessageData(text = it, type = type) }

            HomeScreen(navController = navController, messageData = messageData)
        }
    }
}
