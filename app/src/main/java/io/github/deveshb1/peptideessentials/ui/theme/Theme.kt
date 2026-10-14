package io.github.deveshb1.peptideessentials.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.deveshb1.peptideessentials.R

// Brand palette: deep ink-teal surfaces, mint primary, lavender and amber accents.
private val LightColors = lightColorScheme(
    primary = Color(0xFF00695A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB4F0DF),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF5847B5),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5DEFF),
    onSecondaryContainer = Color(0xFF1A0A5E),
    tertiary = Color(0xFF8A5100),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBC),
    onTertiaryContainer = Color(0xFF2C1600),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5F8F7),
    onBackground = Color(0xFF121C1A),
    surface = Color(0xFFF5F8F7),
    onSurface = Color(0xFF121C1A),
    surfaceVariant = Color(0xFFD7E2DE),
    onSurfaceVariant = Color(0xFF3F4B48),
    outline = Color(0xFF6F7B78),
    outlineVariant = Color(0xFFBFCAC6),
    surfaceDim = Color(0xFFD6DDDA),
    surfaceBright = Color(0xFFF5F8F7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF3F1),
    surfaceContainer = Color(0xFFE8EEEC),
    surfaceContainerHigh = Color(0xFFE2E9E6),
    surfaceContainerHighest = Color(0xFFDCE4E1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EE6C4),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF0F4D40),
    onPrimaryContainer = Color(0xFFA7F5DE),
    secondary = Color(0xFFB9A9FF),
    onSecondary = Color(0xFF26185E),
    secondaryContainer = Color(0xFF352A6B),
    onSecondaryContainer = Color(0xFFE5DEFF),
    tertiary = Color(0xFFFFB86B),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF5C3A10),
    onTertiaryContainer = Color(0xFFFFDDB5),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5C1A16),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0A1113),
    onBackground = Color(0xFFE3ECEA),
    surface = Color(0xFF0A1113),
    onSurface = Color(0xFFE3ECEA),
    surfaceVariant = Color(0xFF24312F),
    onSurfaceVariant = Color(0xFFA9BAB6),
    outline = Color(0xFF6E807C),
    outlineVariant = Color(0xFF2C3B39),
    surfaceDim = Color(0xFF0A1113),
    surfaceBright = Color(0xFF2A3638),
    surfaceContainerLowest = Color(0xFF060B0C),
    surfaceContainerLow = Color(0xFF10191B),
    surfaceContainer = Color(0xFF142022),
    surfaceContainerHigh = Color(0xFF1A282A),
    surfaceContainerHighest = Color(0xFF213133),
)

@OptIn(ExperimentalTextApi::class)
private fun spaceGrotesk(weight: Int) = Font(
    R.font.space_grotesk,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Space Grotesk for headings and labels; the system font stays for body text. */
val DisplayFont = FontFamily(spaceGrotesk(400), spaceGrotesk(500), spaceGrotesk(600), spaceGrotesk(700))

private val Base = Typography()
private val AppTypography = Base.copy(
    displayLarge = Base.displayLarge.copy(fontFamily = DisplayFont, fontWeight = FontWeight.Bold),
    displayMedium = Base.displayMedium.copy(fontFamily = DisplayFont, fontWeight = FontWeight.Bold),
    displaySmall = Base.displaySmall.copy(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold),
    headlineLarge = Base.headlineLarge.copy(fontFamily = DisplayFont, fontWeight = FontWeight.Bold),
    headlineMedium = Base.headlineMedium.copy(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.copy(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontFamily = DisplayFont, fontWeight = FontWeight.Medium),
    labelLarge = Base.labelLarge.copy(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold),
)

/** Warm-to-cool colours for the injection-site heatmap. */
object HeatColors {
    val rested = Color(0xFF3BA7C9)
    val warm = Color(0xFFF2A93B)
    val hot = Color(0xFFE5484D)
}

@Composable
fun AppTheme(
    dynamicColor: Boolean = false,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
