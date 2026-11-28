package au.edu.jcu.cp3406.wordwise.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** Soft, rounded corners: 20 dp cards and 24 dp large surfaces, as in the design system. */
val WordWiseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Buttons use 16 dp corners rather than Material's full pill. */
val ButtonShape = RoundedCornerShape(16.dp)

/**
 * Colours for the app's content cards: white paper on the ivory background in light mode,
 * a slightly raised dark surface in dark mode.
 */
@Composable
fun paperCardColors(): CardColors {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    return CardDefaults.cardColors(
        containerColor = if (isDark) scheme.surfaceContainer else scheme.surfaceContainerLowest,
    )
}
