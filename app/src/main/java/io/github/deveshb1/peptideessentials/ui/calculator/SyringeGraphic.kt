package io.github.deveshb1.peptideessentials.ui.calculator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.calc.Syringe
import io.github.deveshb1.peptideessentials.ui.Format

/** A U-100 barrel drawn to scale, filled to [units]. */
@Composable
fun SyringeGraphic(syringe: Syringe, units: Double, fits: Boolean, ink: Color, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val fill = if (fits) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val labelStyle = TextStyle(fontSize = 10.sp, color = ink.copy(alpha = 0.8f))
    val capacity = syringe.units
    val minorStep = if (capacity <= 50) 1 else 2
    val majorStep = if (capacity <= 50) 5 else 10
    val description = stringResource(R.string.syringe_description, Format.amount(units, 1), capacity)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics { contentDescription = description },
    ) {
        val needle = 28.dp.toPx()
        val plungerRoom = 18.dp.toPx()
        val barrelLeft = needle
        val barrelRight = size.width - plungerRoom
        val barrelWidth = barrelRight - barrelLeft
        val barrelTop = 8.dp.toPx()
        val barrelHeight = 26.dp.toPx()
        val midY = barrelTop + barrelHeight / 2
        val stroke = 1.5.dp.toPx()

        // Needle and hub
        drawLine(ink.copy(alpha = 0.6f), Offset(0f, midY), Offset(barrelLeft - 6.dp.toPx(), midY), strokeWidth = stroke)
        drawRect(ink.copy(alpha = 0.6f), Offset(barrelLeft - 6.dp.toPx(), midY - 4.dp.toPx()), Size(6.dp.toPx(), 8.dp.toPx()))

        // Liquid, from the needle end
        val fraction = (units / capacity).coerceIn(0.0, 1.0).toFloat()
        drawRect(fill.copy(alpha = 0.85f), Offset(barrelLeft, barrelTop), Size(barrelWidth * fraction, barrelHeight))

        // Plunger rod
        val plungerX = barrelLeft + barrelWidth * fraction
        drawRect(ink.copy(alpha = 0.85f), Offset(plungerX, barrelTop + 2.dp.toPx()), Size(3.dp.toPx(), barrelHeight - 4.dp.toPx()))
        drawLine(ink.copy(alpha = 0.5f), Offset(plungerX, midY), Offset(size.width, midY), strokeWidth = 3.dp.toPx())

        drawRoundRect(ink, Offset(barrelLeft, barrelTop), Size(barrelWidth, barrelHeight), CornerRadius(3.dp.toPx()), style = Stroke(stroke))

        // Graduations
        for (u in 0..capacity step minorStep) {
            val x = barrelLeft + barrelWidth * u / capacity
            val major = u % majorStep == 0
            val len = if (major) barrelHeight * 0.55f else barrelHeight * 0.28f
            drawLine(ink.copy(alpha = if (major) 0.9f else 0.5f), Offset(x, barrelTop), Offset(x, barrelTop + len), strokeWidth = 1.dp.toPx())
            if (major && u != 0) {
                val layout = measurer.measure(u.toString(), labelStyle)
                drawText(layout, topLeft = Offset(x - layout.size.width / 2f, barrelTop + barrelHeight + 4.dp.toPx()))
            }
        }
    }
}
