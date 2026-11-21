package ch.hslu.blebooking.mobile.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import ch.hslu.blebooking.mobile.ui.components.AppTopBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ch.hslu.blebooking.mobile.R
import ch.hslu.blebooking.mobile.ui.navigation.AppScreens
import ch.hslu.blebooking.mobile.ui.components.MessageData
import ch.hslu.blebooking.mobile.ui.components.Message
import ch.hslu.blebooking.mobile.ui.components.MessageType
import ch.hslu.blebooking.mobile.ui.theme.BleTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    messageData: MessageData? = null,
    navController: NavController,
    onLogout: () -> Unit,
    isLoggingOut: Boolean = false,
) {
    HomeContent(
        modifier = modifier,
        messageData = messageData,
        isLoading = isLoggingOut,
        onScan = {
            navController.navigate(AppScreens.SCANNING.name) {
                launchSingleTop = true
            }
        },
        onLogout = onLogout,
    )
}
@Composable
private fun HomeContent(
    modifier: Modifier = Modifier,
    messageData: MessageData? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onLogout: () -> Unit = {},
    onScan: () -> Unit = {},
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AppTopBar(title = stringResource(R.string.home_title), showMenu = true,
                enabled = !isLoading, onLogout = onLogout)
            errorMessage?.let { Message(text = it, type = MessageType.Error) }
            if (errorMessage == null) messageData?.let { Message(text = it.text, type = it.type) }
            Button(
                onClick = onScan,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(stringResource(R.string.home_scan_resources))
            }
        }
    }
}

@Preview(name = "Home Light", showBackground = true)
@Composable
private fun HomeLightPreview() {
    BleTheme(darkTheme = false) { HomeContent() }
}

@Preview(name = "Home Dark", showBackground = true)
@Composable
private fun HomeDarkPreview() {
    BleTheme(darkTheme = true) { HomeContent() }
}




