package ch.hslu.blebooking.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    secondary = Color(0xFFB4CCBF), onSecondary = Color(0xFF20352B),
    secondaryContainer = Color(0xFF364C40), onSecondaryContainer = Color(0xFFD0E8DA),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF594300), onTertiaryContainer = Color(0xFFFFDEA0),
    onError = Color(0xFF490006),
    errorContainer = Color(0xFF6E1820), onErrorContainer = Color(0xFFFFDAD6),
    surfaceDim = Color(0xFF101716), surfaceBright = Color(0xFF343C38),
    surfaceContainerLowest = Color(0xFF080D0C),
    surfaceContainerLow = Color(0xFF141D19),
    surfaceContainerHigh = Color(0xFF222D27),
    surfaceContainerHighest = Color(0xFF2D3832),
    outlineVariant = Color(0xFF35433D),
    inverseSurface = Color(0xFFE8EFEC), inverseOnSurface = Color(0xFF14211B),
    inversePrimary = Color(0xFF006C50), surfaceTint = Color(0xFF61DDB2),
    scrim = Color.Black,
    error = Color(0xFFFF6262),
    tertiary = Color(0xFFF2C66D),
    primary = Color(0xFF61DDB2), onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF1B3D2F), onPrimaryContainer = Color(0xFF61DDB2),
    surfaceVariant = Color(0xFF1B2823),
    surfaceContainer = Color(0xFF17211D),
    background = Color(0xFF080D0C), onBackground = Color(0xFFE8EFEC),
    surface = Color(0xFF101716), onSurface = Color(0xFFE8EFEC),
    onSurfaceVariant = Color(0xFF9AA9A3), outline = Color(0xFF35433D),
)
private val LightColors = lightColorScheme(
    secondary = Color(0xFF4E6357), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E8DA), onSecondaryContainer = Color(0xFF20352B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEA0), onTertiaryContainer = Color(0xFF402D00),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF6E1820),
    surfaceDim = Color(0xFFD5DFD8), surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F7F3),
    surfaceContainerHigh = Color(0xFFE5EEE8),
    surfaceContainerHighest = Color(0xFFDFE8E2),
    outlineVariant = Color(0xFFC3D1C8),
    inverseSurface = Color(0xFF29362F), inverseOnSurface = Color(0xFFEDF5EF),
    inversePrimary = Color(0xFF61DDB2), surfaceTint = Color(0xFF006C50),
    scrim = Color.Black,
    error = Color(0xFFBA1A1A),
    tertiary = Color(0xFF795900),
    primary = Color(0xFF006C50), onPrimary = Color.White,
    primaryContainer = Color(0xFFC0F0DB), onPrimaryContainer = Color(0xFF00513B),
    surfaceVariant = Color(0xFFE3EFE8),
    surfaceContainer = Color(0xFFEBF4EE),
    background = Color(0xFFF5FBF7), onBackground = Color(0xFF14211B),
    surface = Color.White, onSurface = Color(0xFF14211B),
    onSurfaceVariant = Color(0xFF52645A), outline = Color(0xFF73867B),
)

/** Pass a saved preference here when app-level appearance settings are available. */
@Composable
fun BleTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
