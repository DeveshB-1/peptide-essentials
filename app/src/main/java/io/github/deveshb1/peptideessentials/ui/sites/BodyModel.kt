package io.github.deveshb1.peptideessentials.ui.sites

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.data.BodyView
import io.github.deveshb1.peptideessentials.data.InjectionSite

// Each figure is drawn in a 100 x 240 unit box; the front view is mirrored like looking at a person.
private const val FIGURE_W = 100f
private const val FIGURE_H = 240f
private const val GAP = 20f

private fun box(l: Float, t: Float, r: Float, b: Float) = Rect(l, t, r, b)

/** Tappable regions in figure units. Person's right is on the viewer's left in the front view only. */
private val regions: Map<InjectionSite, Rect> = mapOf(
    InjectionSite.ABDOMEN_UPPER_RIGHT to box(38f, 74f, 49f, 88f),
    InjectionSite.ABDOMEN_UPPER_LEFT to box(51f, 74f, 62f, 88f),
    InjectionSite.ABDOMEN_LOWER_RIGHT to box(38f, 92f, 49f, 107f),
    InjectionSite.ABDOMEN_LOWER_LEFT to box(51f, 92f, 62f, 107f),
    InjectionSite.THIGH_RIGHT to box(34f, 132f, 46.5f, 164f),
    InjectionSite.THIGH_LEFT to box(53.5f, 132f, 66f, 164f),
    InjectionSite.UPPER_ARM_LEFT to box(19f, 56f, 27f, 86f),
    InjectionSite.UPPER_ARM_RIGHT to box(73f, 56f, 81f, 86f),
    InjectionSite.GLUTE_LEFT to box(36f, 110f, 49.5f, 128f),
    InjectionSite.GLUTE_RIGHT to box(50.5f, 110f, 64f, 128f),
)

private val InjectionSite.figure: Int get() = if (area.view == BodyView.FRONT) 0 else 1

// Right half of the silhouette (viewer's right), from the neck down to the crotch.
private val torsoHalf = listOf(
    50f to 33f, 57f to 34f, 66f to 38f, 72f to 41f, 75f to 47f, 70f to 58f, 68f to 72f, 64f to 96f,
    69f to 112f, 70f to 128f, 68f to 156f, 64f to 178f, 63f to 202f, 59f to 222f, 61f to 229f,
    53f to 229f, 53f to 204f, 53f to 178f, 52f to 142f, 50f to 131f,
)
private val armHalf = listOf(
    72f to 41f, 77f to 47f, 81f to 64f, 82f to 96f, 83f to 124f, 81f to 140f, 76f to 141f,
    75f to 120f, 73f to 96f, 70f to 70f, 70f to 58f,
)

/**
 * Front and back body outlines with injection areas coloured by [siteColor].
 * Hidden from accessibility services: the site chips alongside carry the accessible controls.
 */
@Composable
fun BodyModel(
    siteColor: (InjectionSite) -> Color,
    selected: InjectionSite?,
    suggested: InjectionSite?,
    modifier: Modifier = Modifier,
    onSelect: ((InjectionSite) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val caption = MaterialTheme.typography.labelMedium.copy(color = colors.onSurfaceVariant)
    val captions = listOf(stringResource(R.string.body_front), stringResource(R.string.body_back))
    val left = stringResource(R.string.side_letter_left)
    val right = stringResource(R.string.side_letter_right)
    val currentOnSelect by rememberUpdatedState(onSelect)

    Canvas(
        modifier
            .clearAndSetSemantics { }
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val callback = currentOnSelect ?: return@detectTapGestures
                    val g = Layout(size.width.toFloat(), size.height.toFloat())
                    val slop = 4f
                    regions.entries
                        .firstOrNull { (site, r) -> g.rect(site.figure, r).inflate(slop * g.scale).contains(tap) }
                        ?.let { callback(it.key) }
                }
            },
    ) {
        val g = Layout(size.width, size.height)
        for (figure in 0..1) {
            translate(g.originX(figure), g.originY) {
                drawSilhouette(g.scale, colors.surfaceContainerHighest, colors.outline.copy(alpha = 0.5f))
            }
            val text = measurer.measure(captions[figure], caption)
            drawText(text, topLeft = g.point(figure, 50f, 236f) - Offset(text.size.width / 2f, text.size.height / 2f))
            // Side letters: the front view is mirrored, the back view isn't.
            val (viewerLeft, viewerRight) = if (figure == 0) right to left else left to right
            for ((letter, x) in listOf(viewerLeft to 8f, viewerRight to 92f)) {
                val layout = measurer.measure(letter, caption)
                drawText(layout, topLeft = g.point(figure, x, 200f) - Offset(layout.size.width / 2f, layout.size.height / 2f))
            }
        }
        // Navel as a landmark between the abdomen quadrants.
        drawCircle(colors.outline, radius = 1.4f * g.scale, center = g.point(0, 50f, 90f))

        for ((site, unitRect) in regions) {
            val r = g.rect(site.figure, unitRect)
            val corner = CornerRadius(4f * g.scale)
            drawRoundRect(siteColor(site), r.topLeft, r.size, corner)
            when (site) {
                selected -> drawRoundRect(colors.primary, r.topLeft, r.size, corner, style = Stroke(2.5f * g.scale))
                suggested -> drawRoundRect(
                    colors.onSurface,
                    r.topLeft,
                    r.size,
                    corner,
                    style = Stroke(1.5f * g.scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * g.scale, 2f * g.scale))),
                )
                else -> drawRoundRect(colors.outline.copy(alpha = 0.6f), r.topLeft, r.size, corner, style = Stroke(0.8f * g.scale))
            }
        }
    }
}

private class Layout(width: Float, height: Float) {
    val scale = minOf(height / FIGURE_H, width / (2 * FIGURE_W + GAP))
    private val left = (width - (2 * FIGURE_W + GAP) * scale) / 2f
    val originY = (height - FIGURE_H * scale) / 2f

    fun originX(figure: Int) = left + figure * (FIGURE_W + GAP) * scale

    fun point(figure: Int, x: Float, y: Float) = Offset(originX(figure) + x * scale, originY + y * scale)

    fun rect(figure: Int, r: Rect) = Rect(point(figure, r.left, r.top), Size(r.width * scale, r.height * scale))
}

/** Smooth closed outline through [points] using midpoint quadratic curves. */
private fun smoothPath(points: List<Pair<Float, Float>>, scale: Float): Path {
    val p = points.map { Offset(it.first * scale, it.second * scale) }
    val path = Path()
    val start = (p.last() + p.first()) / 2f
    path.moveTo(start.x, start.y)
    for (i in p.indices) {
        val ctrl = p[i]
        val next = p[(i + 1) % p.size]
        val mid = (ctrl + next) / 2f
        path.quadraticTo(ctrl.x, ctrl.y, mid.x, mid.y)
    }
    path.close()
    return path
}

private fun mirrored(half: List<Pair<Float, Float>>) = half.reversed().map { (x, y) -> (100f - x) to y }

private fun DrawScope.drawSilhouette(scale: Float, fill: Color, outline: Color) {
    val body = smoothPath(torsoHalf + mirrored(torsoHalf), scale)
    val arms = listOf(smoothPath(armHalf, scale), smoothPath(armHalf.map { (x, y) -> (100f - x) to y }, scale))
    val stroke = Stroke(1f * scale)
    for (arm in arms) {
        drawPath(arm, fill)
        drawPath(arm, outline, style = stroke)
    }
    drawPath(body, fill)
    drawPath(body, outline, style = stroke)
    val head = Offset(50f * scale, 17f * scale)
    drawOval(fill, topLeft = head - Offset(11f * scale, 13f * scale), size = Size(22f * scale, 26f * scale))
    drawOval(outline, topLeft = head - Offset(11f * scale, 13f * scale), size = Size(22f * scale, 26f * scale), style = stroke)
    drawRect(fill, topLeft = Offset(45f * scale, 28f * scale), size = Size(10f * scale, 7f * scale))
}
