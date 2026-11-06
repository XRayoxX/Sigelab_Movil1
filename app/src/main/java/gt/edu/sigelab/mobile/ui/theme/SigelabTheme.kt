package gt.edu.sigelab.mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Paleta compartida con TemaSIGELAB.java de la aplicación de escritorio. */
object SigelabColors {
    val Green = Color(0xFF198754)
    val Forest = Color(0xFF155724)
    val Yellow = Color(0xFFFFC107)
    val Navy = Color(0xFF0C1322)
    val Blue = Color(0xFF123D61)
    val Sky = Color(0xFF38BDF8)
}

private val LightColors = lightColorScheme(
    primary = SigelabColors.Green,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEDE2),
    onPrimaryContainer = SigelabColors.Forest,
    secondary = SigelabColors.Yellow,
    onSecondary = Color(0xFF212529),
    secondaryContainer = Color(0xFFFFF2C4),
    onSecondaryContainer = Color(0xFF594300),
    tertiary = Color(0xFF173753),
    onTertiary = Color.White,
    background = Color(0xFFF5FAF5),
    onBackground = Color(0xFF173753),
    surface = Color.White,
    onSurface = Color(0xFF173753),
    surfaceVariant = Color(0xFFEAF2EB),
    onSurfaceVariant = Color(0xFF58718B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F8F5),
    surfaceContainer = Color(0xFFEEF5F0),
    surfaceContainerHigh = Color(0xFFEAF2EB),
    surfaceContainerHighest = Color(0xFFE2ECE5),
    outline = Color(0xFF708477),
    outlineVariant = Color(0xFFCFDBE7),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFFEDEA),
    onErrorContainer = Color(0xFF8C1D18)
)

private val DarkColors = darkColorScheme(
    primary = SigelabColors.Sky,
    onPrimary = SigelabColors.Navy,
    primaryContainer = SigelabColors.Blue,
    onPrimaryContainer = Color(0xFFE5F2FE),
    secondary = SigelabColors.Yellow,
    onSecondary = Color(0xFF212529),
    secondaryContainer = Color(0xFF473A16),
    onSecondaryContainer = Color(0xFFFFE49A),
    tertiary = Color(0xFF14B8A6),
    onTertiary = SigelabColors.Navy,
    background = SigelabColors.Navy,
    onBackground = Color(0xFFE5F2FE),
    surface = Color(0xFF17253B),
    onSurface = Color(0xFFE5F2FE),
    surfaceVariant = Color(0xFF1C3049),
    onSurfaceVariant = Color(0xFF94AFC8),
    surfaceContainerLowest = Color(0xFF0A111E),
    surfaceContainerLow = Color(0xFF0F192A),
    surfaceContainer = Color(0xFF17253B),
    surfaceContainerHigh = Color(0xFF1C3049),
    surfaceContainerHighest = Color(0xFF263B54),
    outline = Color(0xFF7892AB),
    outlineVariant = Color(0xFF2B405D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF451B28),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun SigelabTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(26.dp)
        ),
        content = content
    )
}
