package ch.hslu.blebooking.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ch.hslu.blebooking.mobile.R

@Composable
fun SignalStrength(rssi: Int) {
    val bars = when {
        rssi >= -60 -> 4
        rssi >= -70 -> 3
        rssi >= -80 -> 2
        else -> 1
    }
    val label = stringResource(if (rssi >= -80) R.string.signal_good else R.string.signal_weak)
    val description = stringResource(R.string.signal_description, bars, label)
    val color = if (rssi >= -80) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.height(24.dp).semantics { contentDescription = description },
            verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(4) { index ->
                Box(Modifier.width(5.dp).height((6 * (index + 1)).dp)
                    .background(if (index < bars) color else MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.shapes.extraSmall))
            }
        }
        Text(label, color = color, style = MaterialTheme.typography.labelSmall)
    }
}
