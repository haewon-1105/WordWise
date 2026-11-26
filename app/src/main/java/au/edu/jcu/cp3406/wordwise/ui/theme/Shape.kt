package au.edu.jcu.cp3406.wordwise.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
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
