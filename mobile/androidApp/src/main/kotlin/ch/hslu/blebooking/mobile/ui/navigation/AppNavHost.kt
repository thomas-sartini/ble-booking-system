package ch.hslu.blebooking.mobile.ui.navigation

import androidx.compose.runtime.Composable
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

enum class AppScreens { LOGIN, HOME }

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
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

            HomeScreen(navController = navController, messageData = messageData)
        }
    }
}
