package io.github.deveshb1.peptideessentials.ui.vials

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Ring showing how much of a vial is left, with the percentage in the middle. */
@Composable
fun ProgressRing(fraction: Float, color: Color, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(modifier.size(size).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("${(fraction * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
    }
}
