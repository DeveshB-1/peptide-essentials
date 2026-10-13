package io.github.deveshb1.doselog.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.deveshb1.doselog.R
import io.github.deveshb1.doselog.data.InjectionSite

// Two figures (front and back) drawn in a 100 x 240 unit box each.
private const val FIGURE_W = 100f
private const val FIGURE_H = 240f
private const val GAP = 36f

private data class Marker(val figure: Int, val x: Float, val y: Float)

// Front view mirrors the body: the person's right side is on the viewer's left.
private val markers = mapOf(
    InjectionSite.UPPER_ARM_RIGHT to Marker(0, 19f, 62f),
    InjectionSite.UPPER_ARM_LEFT to Marker(0, 81f, 62f),
    InjectionSite.ABDOMEN_RIGHT to Marker(0, 40f, 94f),
    InjectionSite.ABDOMEN_LEFT to Marker(0, 60f, 94f),
    InjectionSite.THIGH_RIGHT to Marker(0, 39f, 148f),
    InjectionSite.THIGH_LEFT to Marker(0, 61f, 148f),
    InjectionSite.BUTTOCK_LEFT to Marker(1, 39f, 114f),
    InjectionSite.BUTTOCK_RIGHT to Marker(1, 61f, 114f),
)

/**
 * Tappable front/back body outline. Decorative for accessibility services: the site chips
 * next to it carry the accessible controls.
 */
@Composable
fun BodySchematic(
    selected: InjectionSite?,
    suggested: InjectionSite?,
    onSelect: (InjectionSite) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = colors.onSurfaceVariant)
    val captions = listOf(stringResource(R.string.body_front), stringResource(R.string.body_back))
    val sideLetters = stringResource(R.string.side_letter_right) to stringResource(R.string.side_letter_left)
    val currentOnSelect by rememberUpdatedState(onSelect)

    Canvas(
        modifier
            .clearAndSetSemantics { }
            .pointerInput(Unit) {
                val touchRadius = 28.dp.toPx()
                detectTapGestures { tap ->
                    val geometry = Geometry(size.width.toFloat(), size.height.toFloat())
                    markers.entries
                        .map { (site, m) -> site to (geometry.point(m) - tap).getDistance() }
                        .filter { it.second <= touchRadius }
                        .minByOrNull { it.second }
                        ?.let { currentOnSelect(it.first) }
                }
            },
    ) {
        val g = Geometry(size.width, size.height)
        for (figure in 0..1) {
            drawFigure(g, figure, colors.surfaceContainerHighest, colors.outlineVariant)
            val caption = measurer.measure(captions[figure], labelStyle)
            drawText(
                caption,
                topLeft = g.point(Marker(figure, 50f, 232f)) - Offset(caption.size.width / 2f, caption.size.height / 2f),
            )
            // Side letters: front view is mirrored, back view isn't.
            val (leftOfViewer, rightOfViewer) =
                if (figure == 0) sideLetters else sideLetters.second to sideLetters.first
            for ((text, x) in listOf(leftOfViewer to 12f, rightOfViewer to 88f)) {
                val layout = measurer.measure(text, TextStyle(color = colors.outline, fontSize = labelStyle.fontSize))
                drawText(layout, topLeft = g.point(Marker(figure, x, 200f)) - Offset(layout.size.width / 2f, layout.size.height / 2f))
            }
        }
        for ((site, m) in markers) {
            val center = g.point(m)
            when (site) {
                selected -> {
                    drawCircle(colors.primary, radius = 9f * g.scale, center = center)
                    drawCircle(colors.onPrimary, radius = 3f * g.scale, center = center)
                }
                suggested -> {
                    drawCircle(colors.tertiaryContainer, radius = 8f * g.scale, center = center)
                    drawCircle(colors.tertiary, radius = 8f * g.scale, center = center, style = Stroke(2.5f * g.scale))
                }
                else -> {
                    drawCircle(colors.surface, radius = 6f * g.scale, center = center)
                    drawCircle(colors.outline, radius = 6f * g.scale, center = center, style = Stroke(1.5f * g.scale))
                }
            }
        }
    }
}

private class Geometry(width: Float, height: Float) {
    val scale = minOf(height / FIGURE_H, width / (2 * FIGURE_W + GAP))
    private val originX = (width - (2 * FIGURE_W + GAP) * scale) / 2f
    private val originY = (height - FIGURE_H * scale) / 2f

    fun point(m: Marker) = Offset(
        originX + (m.figure * (FIGURE_W + GAP) + m.x) * scale,
        originY + m.y * scale,
    )

    fun rect(figure: Int, left: Float, top: Float, right: Float, bottom: Float): Pair<Offset, Size> {
        val topLeft = point(Marker(figure, left, top))
        return topLeft to Size((right - left) * scale, (bottom - top) * scale)
    }
}

private fun DrawScope.drawFigure(
    g: Geometry,
    figure: Int,
    fill: androidx.compose.ui.graphics.Color,
    outline: androidx.compose.ui.graphics.Color,
) {
    val parts = listOf(
        floatArrayOf(28f, 36f, 72f, 122f, 12f), // torso
        floatArrayOf(13f, 38f, 25f, 112f, 6f), // arms
        floatArrayOf(75f, 38f, 87f, 112f, 6f),
        floatArrayOf(29f, 118f, 49f, 212f, 8f), // legs
        floatArrayOf(51f, 118f, 71f, 212f, 8f),
    )
    for (p in parts) {
        val (topLeft, size) = g.rect(figure, p[0], p[1], p[2], p[3])
        val radius = CornerRadius(p[4] * g.scale)
        drawRoundRect(fill, topLeft, size, radius)
        drawRoundRect(outline, topLeft, size, radius, style = Stroke(1f * g.scale))
    }
    val head = g.point(Marker(figure, 50f, 18f))
    drawCircle(fill, radius = 13f * g.scale, center = head)
    drawCircle(outline, radius = 13f * g.scale, center = head, style = Stroke(1f * g.scale))
}
