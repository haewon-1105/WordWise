package au.edu.jcu.cp3406.wordwise.ui.theme

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

// Brand colours chosen for at least 4.5:1 contrast between text and its container.
private val Navy = Color(0xFF1F4E79)
private val NavyLight = Color(0xFF9FC5E8)
private val Teal = Color(0xFF00696B)
private val TealLight = Color(0xFF80D5D6)
private val Paper = Color(0xFFF8F9FC)
private val Ink = Color(0xFF191C20)
private val Night = Color(0xFF111418)
private val NightText = Color(0xFFE1E2E8)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4F7),
    onPrimaryContainer = Color(0xFF001D35),
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB3EEEF),
    onSecondaryContainer = Color(0xFF002021),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
)

private val DarkColors = darkColorScheme(
    primary = NavyLight,
    onPrimary = Color(0xFF00325A),
    primaryContainer = Color(0xFF16466F),
    onPrimaryContainer = Color(0xFFD3E4F7),
    secondary = TealLight,
    onSecondary = Color(0xFF003738),
    secondaryContainer = Color(0xFF004F51),
    onSecondaryContainer = Color(0xFFB3EEEF),
    background = Night,
    onBackground = NightText,
    surface = Night,
    onSurface = NightText,
)

/**
 * App theme. Dynamic colour (Android 12+) is off by default so contrast stays predictable;
 * it can be switched on later from Settings if wanted.
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
        typography = Typography(),
        content = content,
    )
}
