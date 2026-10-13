package io.github.deveshb1.doselog.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Teal-green fallback palette for devices without dynamic colour.
private val LightColors = lightColorScheme(
    primary = Color(0xFF006B5E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9FF2E0),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF3F6A3A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC0F0B4),
    onSecondaryContainer = Color(0xFF002203),
    tertiary = Color(0xFF3D6377),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1E8FF),
    onTertiaryContainer = Color(0xFF001E2C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5FBF7),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFF5FBF7),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFDAE5E0),
    onSurfaceVariant = Color(0xFF3F4945),
    outline = Color(0xFF6F7975),
    outlineVariant = Color(0xFFBEC9C4),
    surfaceDim = Color(0xFFD5DBD8),
    surfaceBright = Color(0xFFF5FBF7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F1),
    surfaceContainer = Color(0xFFE9EFEC),
    surfaceContainerHigh = Color(0xFFE3EAE6),
    surfaceContainerHighest = Color(0xFFDEE4E0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF83D5C4),
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFF9FF2E0),
    secondary = Color(0xFFA5D39A),
    onSecondary = Color(0xFF103910),
    secondaryContainer = Color(0xFF285024),
    onSecondaryContainer = Color(0xFFC0F0B4),
    tertiary = Color(0xFFA5CCE3),
    onTertiary = Color(0xFF063547),
    tertiaryContainer = Color(0xFF244C5F),
    onTertiaryContainer = Color(0xFFC1E8FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1513),
    onBackground = Color(0xFFDEE4E0),
    surface = Color(0xFF0F1513),
    onSurface = Color(0xFFDEE4E0),
    surfaceVariant = Color(0xFF3F4945),
    onSurfaceVariant = Color(0xFFBEC9C4),
    outline = Color(0xFF89938F),
    outlineVariant = Color(0xFF3F4945),
    surfaceDim = Color(0xFF0F1513),
    surfaceBright = Color(0xFF343B38),
    surfaceContainerLowest = Color(0xFF0A0F0E),
    surfaceContainerLow = Color(0xFF171D1B),
    surfaceContainer = Color(0xFF1B211F),
    surfaceContainerHigh = Color(0xFF252B29),
    surfaceContainerHighest = Color(0xFF303634),
)

@Composable
fun DoseLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
