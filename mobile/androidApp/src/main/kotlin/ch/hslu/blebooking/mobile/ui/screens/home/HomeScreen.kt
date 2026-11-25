package ch.hslu.blebooking.mobile.ui.screens.home

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import ch.hslu.blebooking.mobile.ui.components.AppTopBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import ch.hslu.blebooking.mobile.R
import ch.hslu.blebooking.mobile.ui.navigation.AppScreens
import ch.hslu.blebooking.mobile.ui.components.MessageData
import ch.hslu.blebooking.mobile.ui.components.Message
import ch.hslu.blebooking.mobile.ui.components.MessageType
import ch.hslu.blebooking.mobile.ui.theme.BleTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.hslu.blebooking.mobile.model.UserProfile
import ch.hslu.blebooking.mobile.model.BookingStatus
import ch.hslu.blebooking.mobile.ui.components.BookingCard
import ch.hslu.blebooking.mobile.ui.components.BookingsFeedback
import ch.hslu.blebooking.mobile.ui.screens.bookings.BookingsState
import ch.hslu.blebooking.mobile.ui.screens.bookings.BookingsViewModel
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    messageData: MessageData? = null,
    navController: NavController,
    homeViewModel: HomeViewModel = hiltViewModel(),
    bookingsViewModel: BookingsViewModel = hiltViewModel(),
) {
    val successText = stringResource(R.string.logout_success)
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val userProfile by homeViewModel.userProfile.collectAsStateWithLifecycle()

    val bookingsState by bookingsViewModel.state.collectAsStateWithLifecycle()

    HomeContent(
        modifier = modifier,
        messageData = messageData,
        isLoading = homeViewModel.isLoading,
        errorMessage = errorMessage,
        userProfile = userProfile,
        bookingsState = bookingsState,
        onRetryBookings = bookingsViewModel::loadBookings,
        onSeeAllBookings = { navController.navigate(AppScreens.BOOKINGS.name) { launchSingleTop = true } },
        onLogout = {
            errorMessage = null
            homeViewModel.logout(
                onSuccess = {
                    navController.navigate("${AppScreens.LOGIN.name}?message=${Uri.encode(successText)}&type=${MessageType.Success.name}") {
                        popUpTo("${AppScreens.HOME.name}?message={message}&type={type}") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onError = { message -> errorMessage = message },
            )
        },
    )
}

@Composable
private fun HomeContent(
    modifier: Modifier = Modifier,
    messageData: MessageData? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    userProfile: UserProfile? = null,
    onLogout: () -> Unit = {},
    bookingsState: BookingsState = BookingsState.Loading,
    onRetryBookings: () -> Unit = {},
    onSeeAllBookings: () -> Unit = {},
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top Bar with menu & logout
            AppTopBar(title = stringResource(R.string.login_brand), showMenu = true, enabled = !isLoading, onLogout = onLogout)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Error and message banners
                errorMessage?.let { Message(text = it, type = MessageType.Error) }
                if (errorMessage == null) messageData?.let { Message(text = it.text, type = it.type) }

                // User Header Row (Avatar, Name, Online Badge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // User Avatar Icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👤",
                                fontSize = 20.sp
                            )
                        }

                        Column {
                            Text(
                                text = stringResource(R.string.home_welcome_back),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val displayName = userProfile?.let {
                                "${it.firstName} ${it.lastName}".trim()
                            }.orEmpty()
                            if (displayName.isNotEmpty()) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    // Online Badge Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onPrimaryContainer)
                            )
                            Text(
                                text = stringResource(R.string.home_status_online),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Section Title & Subtitle
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                }

                BookingsFeedback(bookingsState, onRetryBookings)
                val bookings = (bookingsState as? BookingsState.Ready)?.bookings.orEmpty()
                val featured = bookings.firstOrNull { it.status == BookingStatus.CHECKED_IN }
                    ?: bookings.firstOrNull()
                featured?.let { BookingCard(it) }
                TextButton(onClick = onSeeAllBookings, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.bookings_see_all), textDecoration = TextDecoration.Underline)
                }
            }

            // Bottom Section: Bluetooth status + Connect Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bluetooth Active Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ᛒ",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = stringResource(R.string.home_bluetooth_active),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Connect to Terminal Button (non fa nulla per ora)
                Button(
                    onClick = { /* Non deve fare nulla per ora */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_connect_terminal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ᛒ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
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




