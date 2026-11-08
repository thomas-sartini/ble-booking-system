package ch.hslu.blebooking.mobile

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import ch.hslu.blebooking.mobile.ui.navigation.AppNavHost

@Composable
fun App() {
    val navController = rememberNavController()
    AppNavHost(navController = navController)
}
