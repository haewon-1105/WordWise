package au.edu.jcu.cp3406.wordwise.ui.theme

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

/*
 * "Calm Academic" palette: forest green on warm ivory, with sage accents.
 * Contrast checked with the WCAG formula:
 *  - text on its background or container is at least 4.5:1 (most pairs are above 7:1);
 *  - chart bars (tertiary) are at least 3:1 against the cards they sit on (WCAG 1.4.11).
 */

// Light
private val Forest = Color(0xFF344D40)      // 9.2:1 on white
private val Sage = Color(0xFFDCE7DC)
private val Moss = Color(0xFF4F6B59)        // 5.9:1 on white, for secondary text and buttons
private val MutedSage = Color(0xFF6F8C79)   // 3.4:1 on ivory, chart bars only
private val Ivory = Color(0xFFF7F6F0)
private val Paper = Color(0xFFFFFFFF)
private val Ink = Color(0xFF252B27)          // 13.3:1 on ivory
private val InkSoft = Color(0xFF4A524C)      // 7.4:1 on ivory
private val Brick = Color(0xFFA3392F)        // 6.6:1 on white

// Dark
private val Night = Color(0xFF141815)
private val NightCard = Color(0xFF1E2420)
private val Leaf = Color(0xFFA9C9B4)         // 10:1 on Night
private val NightText = Color(0xFFE3E6E0)    // 14.2:1 on Night

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Paper,
    primaryContainer = Sage,
    onPrimaryContainer = Color(0xFF10201A),
    inversePrimary = Leaf,
    secondary = Moss,
    onSecondary = Paper,
    secondaryContainer = Color(0xFFE3EBE2),
    onSecondaryContainer = Color(0xFF1B2B22),
    tertiary = MutedSage,
    onTertiary = Color(0xFF10201A),
    tertiaryContainer = Color(0xFFEFE8D6),
    onTertiaryContainer = Color(0xFF2E2614),
    background = Ivory,
    onBackground = Ink,
    surface = Ivory,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE4E8E1),
    onSurfaceVariant = InkSoft,
    surfaceTint = Forest,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Color(0xFFF2F2EB),
    surfaceContainer = Color(0xFFEEEEE6),
    surfaceContainerHigh = Color(0xFFE8E9E1),
    surfaceContainerHighest = Color(0xFFE2E4DC),
    outline = Color(0xFF737B74),             // 4.4:1, used for borders
    outlineVariant = Color(0xFFC4CABF),
    error = Brick,
    onError = Paper,
    errorContainer = Color(0xFFF8DAD5),
    onErrorContainer = Color(0xFF410E09),
)

private val DarkColors = darkColorScheme(
    primary = Leaf,
    onPrimary = Color(0xFF12301F),
    primaryContainer = Color(0xFF2B4436),
    onPrimaryContainer = Color(0xFFD2E6D7),
    inversePrimary = Forest,
    secondary = Color(0xFFB7CDBD),
    onSecondary = Color(0xFF1F3527),
    secondaryContainer = Color(0xFF34473B),
    onSecondaryContainer = Color(0xFFD3E7D8),
    tertiary = Color(0xFF7F9C89),            // 5.3:1 on dark cards
    onTertiary = Color(0xFF0E1A13),
    tertiaryContainer = Color(0xFF4A4330),
    onTertiaryContainer = Color(0xFFEFE8D6),
    background = Night,
    onBackground = NightText,
    surface = Night,
    onSurface = NightText,
    surfaceVariant = Color(0xFF3D453F),
    onSurfaceVariant = Color(0xFFC1C8C0),
    surfaceTint = Leaf,
    surfaceContainerLowest = Color(0xFF0F1210),
    surfaceContainerLow = Color(0xFF1A1F1B),
    surfaceContainer = NightCard,
    surfaceContainerHigh = Color(0xFF282E29),
    surfaceContainerHighest = Color(0xFF323933),
    outline = Color(0xFF8B938B),
    outlineVariant = Color(0xFF3D453F),
    error = Color(0xFFF2B8B0),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF7F2A22),
    onErrorContainer = Color(0xFFF8DAD5),
)

/**
 * App theme. Dynamic colour (Android 12+) stays off so the brand palette and its checked
 * contrast are what every learner sees. Dark mode follows the system setting.
 */
@Composable
fun WordWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WordWiseTypography,
        shapes = WordWiseShapes,
        content = content,
    )
}
