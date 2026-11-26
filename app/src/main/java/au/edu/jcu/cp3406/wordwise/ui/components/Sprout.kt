package au.edu.jcu.cp3406.wordwise.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * WordWise's small brand illustration: a sprout, for steady growth rather than a reward burst.
 * Drawn in code from theme colours, so it follows dark mode and needs no image file.
 * It is decorative, so it has no semantics and TalkBack skips it.
 */
@Composable
fun Sprout(modifier: Modifier = Modifier, size: Dp = 152.dp) {
    val background = MaterialTheme.colorScheme.primaryContainer
    val leaf = MaterialTheme.colorScheme.tertiary
    val line = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        fun p(x: Float, y: Float) = Offset(x * w, y * h)
        val stroke = Stroke(width = w * 0.035f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawCircle(color = background, radius = w / 2f)

        val leftLeaf = Path().apply {
            moveTo(0.5f * w, 0.64f * h)
            cubicTo(0.44f * w, 0.46f * h, 0.32f * w, 0.40f * h, 0.20f * w, 0.40f * h)
            cubicTo(0.20f * w, 0.55f * h, 0.32f * w, 0.64f * h, 0.5f * w, 0.64f * h)
            close()
        }
        val rightLeaf = Path().apply {
            moveTo(0.5f * w, 0.52f * h)
            cubicTo(0.53f * w, 0.36f * h, 0.64f * w, 0.28f * h, 0.80f * w, 0.26f * h)
            cubicTo(0.80f * w, 0.42f * h, 0.69f * w, 0.52f * h, 0.5f * w, 0.52f * h)
            close()
        }
        listOf(leftLeaf, rightLeaf).forEach { shape ->
            drawPath(shape, color = leaf.copy(alpha = 0.55f))
            drawPath(shape, color = line, style = stroke)
        }
        drawLine(line, p(0.5f, 0.80f), p(0.5f, 0.50f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(line, p(0.32f, 0.80f), p(0.68f, 0.80f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}
