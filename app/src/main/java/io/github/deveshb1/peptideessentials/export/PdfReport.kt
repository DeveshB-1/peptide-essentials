package io.github.deveshb1.peptideessentials.export

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.DoseStatus
import io.github.deveshb1.peptideessentials.progress.MetricMath
import io.github.deveshb1.peptideessentials.report.AdherenceCalc
import io.github.deveshb1.peptideessentials.ui.Format
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * A printable summary for a care team: medications and schedules, adherence, every dose,
 * measurement trends and side-effect counts for a date range.
 */
object PdfReport {

    fun create(context: Context, data: AppData, from: LocalDate, to: LocalDate, today: LocalDate): File {
        val res = context.resources
        val zone = ZoneId.systemDefault()
        val doc = PdfDocument()
        val w = Writer(doc, context.getString(R.string.app_name))

        w.title(context.getString(R.string.report_title))
        w.text(context.getString(R.string.report_period, Format.longDate(from, today), Format.longDate(to, today)))
        w.text(context.getString(R.string.report_generated, Format.longDate(today, today)), w.small)
        w.text(context.getString(R.string.report_disclaimer), w.small)

        w.heading(context.getString(R.string.report_medications))
        if (data.medications.isEmpty()) w.text(context.getString(R.string.report_none))
        data.medications.sortedBy { it.name.lowercase() }.forEach { med ->
            val line = context.getString(
                R.string.dose_schedule_line,
                Format.dose(res, med.doseAmount, med.unit),
                Format.schedule(res, med.schedule),
                Format.time(context, med.timeOfDay),
            ) + if (med.paused) " · " + context.getString(R.string.paused) else ""
            w.text(med.name, w.bold)
            w.text(line)
            if (med.notes.isNotBlank()) w.text(med.notes, w.small)
        }

        w.heading(context.getString(R.string.report_adherence))
        val adherenceWidths = listOf(0.40f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f)
        w.row(
            listOf(
                context.getString(R.string.medication),
                context.getString(R.string.report_scheduled),
                context.getString(R.string.status_taken),
                context.getString(R.string.status_skipped),
                context.getString(R.string.report_missed),
                "%",
            ),
            adherenceWidths,
            w.bold,
        )
        data.medications.sortedBy { it.name.lowercase() }.forEach { med ->
            val a = AdherenceCalc.compute(med, data.entries, from, to, today)
            w.row(
                listOf(med.name, "${a.scheduled}", "${a.taken}", "${a.skipped}", "${a.missed}", a.percent?.let { "$it%" } ?: "–"),
                adherenceWidths,
                w.body,
            )
        }
        w.text(context.getString(R.string.report_adherence_note), w.small)

        w.heading(context.getString(R.string.report_doses))
        val doses = data.entries.filter { it.displayDate(zone) in from..to }.sortedBy { it.time }
        if (doses.isEmpty()) w.text(context.getString(R.string.report_none))
        val doseWidths = listOf(0.22f, 0.30f, 0.14f, 0.34f)
        if (doses.isNotEmpty()) {
            w.row(
                listOf(
                    context.getString(R.string.date),
                    context.getString(R.string.medication),
                    context.getString(R.string.dose_amount),
                    context.getString(R.string.report_status_site),
                ),
                doseWidths,
                w.bold,
            )
        }
        doses.forEach { e ->
            val local = e.time.atZone(zone)
            val taken = e.status == DoseStatus.TAKEN
            w.row(
                listOf(
                    Format.shortDate(local.toLocalDate()) + if (taken) " " + Format.time(context, local.toLocalTime()) else "",
                    e.medicationName,
                    if (taken) Format.dose(res, e.amount, e.unit) else "",
                    if (taken) e.site?.let { Format.site(res, it) } ?: context.getString(R.string.status_taken)
                    else context.getString(R.string.status_skipped),
                ),
                doseWidths,
                w.body,
            )
        }

        w.heading(context.getString(R.string.report_measurements))
        var anyMetric = false
        data.metrics.forEach { metric ->
            val points = data.measurements.filter { it.metricId == metric.id && it.date in from..to }.sortedBy { it.date }
            if (points.isEmpty()) return@forEach
            anyMetric = true
            val first = points.first()
            val last = points.last()
            fun shown(v: Double) = Format.metricValue(res, MetricMath.toDisplay(v, metric.kind, data.settings), metric, data.settings)
            val delta = MetricMath.toDisplay(last.value, metric.kind, data.settings) - MetricMath.toDisplay(first.value, metric.kind, data.settings)
            w.text(Format.metricName(res, metric), w.bold)
            w.text(
                context.getString(
                    R.string.report_metric_line,
                    shown(first.value),
                    Format.shortDate(first.date),
                    shown(last.value),
                    Format.shortDate(last.date),
                    Format.signed(delta),
                    points.size,
                ),
            )
        }
        if (!anyMetric) w.text(context.getString(R.string.report_none))

        w.heading(context.getString(R.string.report_side_effects))
        val journal = data.journal.filter { it.date in from..to }
        val counts = journal.flatMap { it.sideEffects }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }
        if (counts.isEmpty()) {
            w.text(context.getString(R.string.report_none))
        } else {
            counts.forEach { (key, n) ->
                w.text(res.getQuantityString(R.plurals.report_days, n, Format.sideEffect(res, key), n))
            }
        }
        w.text(res.getQuantityString(R.plurals.report_journal_days, journal.size, journal.size), w.small)

        w.finish()
        val file = ShareFile.newFile(context, "peptide-essentials-report-$from-to-$to.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /** Lays text out top to bottom on A4 pages, starting a new page when one fills up. */
    private class Writer(private val doc: PdfDocument, private val footer: String) {
        private val pageWidth = 595
        private val pageHeight = 842
        private val margin = 44f
        private val contentWidth = pageWidth - 2 * margin
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null
        private var y = 0f

        private val accent = Color.rgb(0, 105, 90)
        val body = paint(10f)
        val bold = paint(10f, Typeface.DEFAULT_BOLD)
        val small = paint(8.5f).apply { color = Color.DKGRAY }
        private val headingPaint = paint(13f, Typeface.DEFAULT_BOLD).apply { color = accent }
        private val titlePaint = paint(20f, Typeface.DEFAULT_BOLD).apply { color = accent }

        private fun paint(size: Float, face: Typeface = Typeface.DEFAULT) = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            typeface = face
            color = Color.BLACK
        }

        private fun canvas() = (page ?: newPage()).canvas

        private fun newPage(): PdfDocument.Page {
            page?.let(::closePage)
            pageNumber++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            page = p
            y = margin
            return p
        }

        private fun closePage(p: PdfDocument.Page) {
            p.canvas.drawText("$footer · $pageNumber", margin, pageHeight - margin / 2, small)
            doc.finishPage(p)
        }

        private fun ensure(height: Float) {
            if (page == null || y + height > pageHeight - margin) newPage()
        }

        fun title(text: String) = text(text, titlePaint, after = 6f)

        fun heading(text: String) {
            ensure(48f)
            y += 12f
            text(text, headingPaint, after = 2f)
            canvas().drawLine(margin, y, margin + contentWidth, y, headingPaint)
            y += 6f
        }

        fun text(text: String, paint: TextPaint = body, after: Float = 3f) {
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            ensure(layout.height.toFloat())
            val c = canvas()
            c.save()
            c.translate(margin, y)
            layout.draw(c)
            c.restore()
            y += layout.height + after
        }

        /** One table row; cells are ellipsized to their share of the width. */
        fun row(cells: List<String>, widths: List<Float>, paint: TextPaint) {
            val height = paint.textSize * 1.5f
            ensure(height)
            val c = canvas()
            var x = margin
            cells.forEachIndexed { i, cell ->
                val width = contentWidth * widths[i]
                val shown = TextUtils.ellipsize(cell, paint, width - 6f, TextUtils.TruncateAt.END).toString()
                c.drawText(shown, x, y + paint.textSize, paint)
                x += width
            }
            y += height
        }

        fun finish() {
            page?.let(::closePage)
            page = null
            if (pageNumber == 0) {
                newPage()
                page?.let(::closePage)
            }
        }
    }
}
