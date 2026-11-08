package ch.hslu.blebooking.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    error = Color(0xFFFF6262),
    tertiary = Color(0xFFF2C66D),
    primary = Color(0xFF61DDB2), onPrimary = Color(0xFF00382A),
    background = Color(0xFF080D0C), onBackground = Color(0xFFE8EFEC),
    surface = Color(0xFF101716), onSurface = Color(0xFFE8EFEC),
    onSurfaceVariant = Color(0xFF9AA9A3), outline = Color(0xFF35433D),
)
private val LightColors = lightColorScheme(
    error = Color(0xFFBA1A1A),
    tertiary = Color(0xFF795900),
    primary = Color(0xFF006C50), onPrimary = Color.White,
    background = Color(0xFFF5FBF7), onBackground = Color(0xFF14211B),
    surface = Color.White, onSurface = Color(0xFF14211B),
    onSurfaceVariant = Color(0xFF52645A), outline = Color(0xFF73867B),
)

/** Pass a saved preference here when app-level appearance settings are available. */
@Composable
fun BleTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
