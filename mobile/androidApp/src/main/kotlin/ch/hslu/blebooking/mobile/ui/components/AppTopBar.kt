package ch.hslu.blebooking.mobile.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import ch.hslu.blebooking.mobile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    showMenu: Boolean = false,
    enabled: Boolean = true,
    onLogout: () -> Unit = {},
    onRefresh: (() -> Unit)? = null,
    refreshEnabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    CenterAlignedTopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack, enabled = enabled) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.navigate_back))
                }
            }
        },
        actions = {
            if (onRefresh != null) {
                IconButton(onClick = onRefresh, enabled = enabled && refreshEnabled) {
                    Icon(painterResource(R.drawable.ic_refresh), stringResource(R.string.bookings_refresh))
                }
            }
            if (showMenu) {
                Box {
                    IconButton(onClick = { expanded = true }, enabled = enabled) {
                        Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.menu_options))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.logout)) },
                            onClick = { expanded = false; onLogout() },
                            enabled = enabled,
                        )
                    }
                }
            }
        },
    )
}
