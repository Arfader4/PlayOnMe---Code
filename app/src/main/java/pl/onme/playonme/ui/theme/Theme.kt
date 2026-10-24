package pl.onme.playonme.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

// Ciemny motyw, wspólny charakter z innymi projektami „OnMe”.
private val OnMeDark = darkColorScheme(
    primary = Color(0xFF7AA2F7),
    onPrimary = Color(0xFF0F1115),
    secondary = Color(0xFF9ECE6A),
    tertiary = Color(0xFFE0AF68),
    error = Color(0xFFF7768E),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF171A21),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF1F232C),
    onSurfaceVariant = Color(0xFFB0B4BE),
)

/** Styl dla bloków kodu w rundzie. */
val CodeTextStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 20.sp)

@Composable
fun PlayOnMeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OnMeDark, typography = Typography(), content = content)
}
