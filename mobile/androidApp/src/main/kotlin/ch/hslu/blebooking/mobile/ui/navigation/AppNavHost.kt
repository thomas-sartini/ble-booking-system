package ch.hslu.blebooking.mobile.ui.navigation

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import ch.hslu.blebooking.mobile.R
import ch.hslu.blebooking.mobile.ui.screens.home.HomeViewModel
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ch.hslu.blebooking.mobile.ui.components.MessageData
import ch.hslu.blebooking.mobile.ui.components.MessageType
import ch.hslu.blebooking.mobile.ui.screens.home.HomeScreen
import ch.hslu.blebooking.mobile.ui.screens.login.LoginScreen
import ch.hslu.blebooking.mobile.ui.screens.scanning.ScanningView
import ch.hslu.blebooking.mobile.ui.screens.terminal.TerminalView

enum class AppScreens { LOGIN, HOME, SCANNING, TERMINAL }

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val sessionViewModel: HomeViewModel = hiltViewModel()
    val logoutSuccess = stringResource(R.string.logout_success)
    var logoutError by remember { mutableStateOf<String?>(null) }
    val onLogout: () -> Unit = {
        sessionViewModel.logout(
            onSuccess = {
                navController.navigate("${AppScreens.LOGIN.name}?message=${Uri.encode(logoutSuccess)}&type=${MessageType.Success.name}") {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onError = { logoutError = it },
        )
    }
    logoutError?.let { message ->
        AlertDialog(
            onDismissRequest = { logoutError = null },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { logoutError = null }) { Text(stringResource(android.R.string.ok)) } },
        )
    }
    NavHost(
        navController = navController,
        startDestination = AppScreens.LOGIN.name,
        modifier = modifier,
    ) {
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

            HomeScreen(navController = navController, messageData = messageData,
                onLogout = onLogout, isLoggingOut = sessionViewModel.isLoading)
        }
        composable(route = AppScreens.SCANNING.name) {
            ScanningView(onBack = { navController.popBackStack() },
                onLogout = onLogout, isLoggingOut = sessionViewModel.isLoading, onTerminalSelected = { terminal ->
                navController.navigate(TerminalRoute.create(terminal)) {
                    launchSingleTop = true
                }
            })
        }
        composable(route = TerminalRoute.pattern, arguments = TerminalRoute.arguments) {
            TerminalView(onBack = { navController.popBackStack() },
                onLogout = onLogout, isLoggingOut = sessionViewModel.isLoading)
        }
    }
}
