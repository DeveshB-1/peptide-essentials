package io.github.deveshb1.peptideessentials.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.ui.Format
import java.time.LocalDate

/**
 * Line chart of dated values between [from] and [to], with small ticks under the axis on dose days.
 * Drawn directly on a Canvas; [description] is read out by accessibility services instead.
 */
@Composable
fun LineChart(
    points: List<Pair<LocalDate, Double>>,
    from: LocalDate,
    to: LocalDate,
    doseDays: Set<LocalDate>,
    color: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val markerColor = colors.secondary

    Canvas(modifier.semantics { contentDescription = description }) {
        if (points.isEmpty()) return@Canvas
        val values = points.map { it.second }
        var minY = values.min()
        var maxY = values.max()
        if (maxY - minY < 1e-9) {
            minY -= 1.0
            maxY += 1.0
        }
        val pad = (maxY - minY) * 0.12
        minY -= pad
        maxY += pad

        val axisLabelWidth = measurer.measure(Format.amount(maxY, 1), labelStyle).size.width + 6.dp.toPx()
        val markerBand = 10.dp.toPx()
        val dateBand = measurer.measure("0", labelStyle).size.height + 4.dp.toPx()
        val left = axisLabelWidth
        val right = size.width - 6.dp.toPx()
        val top = 6.dp.toPx()
        val bottom = size.height - markerBand - dateBand
        val span = (to.toEpochDay() - from.toEpochDay()).coerceAtLeast(1)

        fun x(date: LocalDate) = left + (right - left) * (date.toEpochDay() - from.toEpochDay()).toFloat() / span
        fun y(value: Double) = (bottom - (bottom - top) * ((value - minY) / (maxY - minY))).toFloat()

        // Grid with value labels
        for (i in 0..2) {
            val v = minY + (maxY - minY) * (0.15 + 0.35 * i)
            val gy = y(v)
            drawLine(colors.outlineVariant, Offset(left, gy), Offset(right, gy), strokeWidth = 1.dp.toPx())
            val label = measurer.measure(Format.amount(v, 1), labelStyle)
            drawText(label, topLeft = Offset(left - label.size.width - 4.dp.toPx(), gy - label.size.height / 2f))
        }

        // Dose-day markers along the time axis
        for (day in doseDays) {
            if (day < from || day > to) continue
            val mx = x(day)
            drawLine(markerColor, Offset(mx, bottom + 3.dp.toPx()), Offset(mx, bottom + markerBand - 1.dp.toPx()), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        }

        // Start and end dates
        val start = measurer.measure(Format.shortDate(from), labelStyle)
        drawText(start, topLeft = Offset(left, bottom + markerBand))
        val end = measurer.measure(Format.shortDate(to), labelStyle)
        drawText(end, topLeft = Offset(right - end.size.width, bottom + markerBand))

        val sorted = points.sortedBy { it.first }
        val line = Path()
        sorted.forEachIndexed { i, (d, v) -> if (i == 0) line.moveTo(x(d), y(v)) else line.lineTo(x(d), y(v)) }
        if (sorted.size > 1) {
            val area = Path().apply {
                addPath(line)
                lineTo(x(sorted.last().first), bottom)
                lineTo(x(sorted.first().first), bottom)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0f)), startY = top, endY = bottom))
            drawPath(line, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        sorted.forEachIndexed { i, (d, v) ->
            val last = i == sorted.lastIndex
            drawCircle(color, radius = if (last) 5.dp.toPx() else 3.dp.toPx(), center = Offset(x(d), y(v)))
            if (last) drawCircle(colors.surface, radius = 2.dp.toPx(), center = Offset(x(d), y(v)))
        }
    }
}
