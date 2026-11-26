package au.edu.jcu.cp3406.wordwise.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Serif for words and big headings (the "academic" voice), the system sans-serif for
 * everything you read quickly or tap. Both are system fonts, so nothing is downloaded and
 * every size is in sp, which means the user's font-size setting always applies.
 */
private val Display = FontFamily.Serif
private val Body = FontFamily.Default

private val Base = Typography()

private fun TextStyle.serif(weight: FontWeight = FontWeight.Normal) =
    copy(fontFamily = Display, fontWeight = weight, letterSpacing = 0.sp)

val WordWiseTypography = Typography(
    // Flashcard term and Home hero line.
    displayLarge = Base.displayLarge.serif(),
    displayMedium = Base.displayMedium.serif(),
    displaySmall = Base.displaySmall.serif(FontWeight.Medium),
    // Screen titles.
    headlineLarge = Base.headlineLarge.serif(FontWeight.Medium),
    headlineMedium = Base.headlineMedium.serif(FontWeight.Medium),
    headlineSmall = Base.headlineSmall.serif(FontWeight.Medium),
    // Card titles, list items and buttons stay sans-serif for quick reading.
    titleLarge = Base.titleLarge.copy(fontFamily = Body, fontWeight = FontWeight.Medium),
    titleMedium = Base.titleMedium.copy(fontFamily = Body, fontWeight = FontWeight.Medium),
    titleSmall = Base.titleSmall.copy(fontFamily = Body),
    bodyLarge = Base.bodyLarge.copy(fontFamily = Body),
    bodyMedium = Base.bodyMedium.copy(fontFamily = Body),
    bodySmall = Base.bodySmall.copy(fontFamily = Body),
    labelLarge = Base.labelLarge.copy(fontFamily = Body, fontWeight = FontWeight.Medium),
    labelMedium = Base.labelMedium.copy(fontFamily = Body),
    // Small capitals-style section labels ("TODAY'S GOAL", "YOUR DATA").
    labelSmall = Base.labelSmall.copy(fontFamily = Body, letterSpacing = 1.sp),
)
