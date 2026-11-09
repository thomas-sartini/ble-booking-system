package ch.hslu.blebooking.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.hslu.blebooking.mobile.ui.theme.BleTheme

enum class MessageType { Success, Error, Warning }

@Composable
fun Message(
    text: String,
    type: MessageType,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val accent = when (type) {
        MessageType.Success -> colors.primary
        MessageType.Error -> colors.error
        MessageType.Warning -> colors.tertiary
    }
    val symbol = when (type) {
        MessageType.Success -> "✓"
        MessageType.Error -> "!"
        MessageType.Warning -> "⚠"
    }
    Surface(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        color = accent.copy(alpha = if (type == MessageType.Error) 0.18f else 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = if (type == MessageType.Error) 0.6f else 0.35f)),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(symbol, color = accent, fontWeight = FontWeight.Bold)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text, color = accent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                description?.let {
                    Text(it, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Preview(name = "Messages Light", showBackground = true)
@Composable
private fun MessagesLightPreview() {
    BleTheme(darkTheme = false) { MessageExamples() }
}

@Preview(name = "Messages Dark", showBackground = true)
@Composable
private fun MessagesDarkPreview() {
    BleTheme(darkTheme = true) { MessageExamples() }
}

@Composable
private fun MessageExamples() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Message("alex@company.com", MessageType.Success)
            Message("Password too short", MessageType.Error, description = "Use at least 8 characters.")
            Message("Session expiring soon", MessageType.Warning, description = "Sign in again to continue.")
        }
    }
}
