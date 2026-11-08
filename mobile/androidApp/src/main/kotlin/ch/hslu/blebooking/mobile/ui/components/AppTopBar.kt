package ch.hslu.blebooking.mobile.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.hslu.blebooking.mobile.R

@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    showMenu: Boolean = false,
    enabled: Boolean = true,
    onLogout: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.login_brand), modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
        if (showMenu) {
            Box {
                val menuLabel = stringResource(R.string.menu_options)
                IconButton(onClick = { expanded = true }, enabled = enabled,
                    modifier = Modifier.semantics { contentDescription = menuLabel }) {
                    Text("⋮", fontSize = 28.sp)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.logout)) }, onClick = {
                        expanded = false
                        onLogout()
                    }, enabled = enabled)
                }
            }
        }
    }
}
