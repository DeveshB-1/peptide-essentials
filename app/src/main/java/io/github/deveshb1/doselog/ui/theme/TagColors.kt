package io.github.deveshb1.doselog.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import io.github.deveshb1.doselog.R

/** Colour tags users can give a medication. Mid-tones that read on light and dark surfaces. */
object TagColors {
    data class Tag(val argb: Int, @param:StringRes val name: Int) {
        val color: Color get() = Color(argb)
    }

    val all = listOf(
        Tag(0xFF14A08F.toInt(), R.string.color_teal),
        Tag(0xFF4CAF50.toInt(), R.string.color_green),
        Tag(0xFF3F7FD9.toInt(), R.string.color_blue),
        Tag(0xFF8E63CE.toInt(), R.string.color_purple),
        Tag(0xFFD8578C.toInt(), R.string.color_pink),
        Tag(0xFFE0533D.toInt(), R.string.color_red),
        Tag(0xFFEF8A2B.toInt(), R.string.color_orange),
        Tag(0xFFC9A227.toInt(), R.string.color_amber),
    )

    fun of(index: Int): Tag = all[index.mod(all.size)]

    fun argb(index: Int): Int = of(index).argb
}
