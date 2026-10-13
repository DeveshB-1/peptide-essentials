package io.github.deveshb1.doselog.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.github.deveshb1.doselog.R
import io.github.deveshb1.doselog.data.AppData
import io.github.deveshb1.doselog.data.DoseStatus
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvExport {
    private val HEADER = listOf(
        "date", "time", "medication", "status", "amount", "unit", "site", "scheduled_date", "note",
    )
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    /** Full history, newest first, with times in [zone]. */
    fun build(data: AppData, zone: ZoneId): String = buildString {
        appendRow(HEADER)
        for (entry in data.entries.sortedByDescending { it.time }) {
            val local = entry.time.atZone(zone)
            appendRow(
                listOf(
                    local.toLocalDate().toString(),
                    if (entry.status == DoseStatus.TAKEN) local.format(timeFormat) else "",
                    entry.medicationName,
                    entry.status.name.lowercase(),
                    if (entry.status == DoseStatus.TAKEN) entry.amount.toBigDecimal().stripTrailingZeros().toPlainString() else "",
                    entry.unit.key,
                    entry.site?.name?.lowercase().orEmpty(),
                    entry.scheduledDate?.toString().orEmpty(),
                    entry.note,
                ),
            )
        }
    }

    fun share(context: Context, data: AppData) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "dose-log-history-${LocalDate.now()}.csv")
        file.writeText(build(data, ZoneId.systemDefault()))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_subject))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(file.name, uri)
        context.startActivity(Intent.createChooser(send, context.getString(R.string.export_csv)))
    }

    private fun StringBuilder.appendRow(cells: List<String>) {
        cells.joinTo(this, ",") { escape(it) }
        append("\r\n")
    }

    private fun escape(cell: String): String {
        // Leading formula characters are neutralised so spreadsheets don't evaluate user text.
        val safe = if (cell.isNotEmpty() && cell[0] in "=+-@") "'$cell" else cell
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + safe.replace("\"", "\"\"") + "\""
        } else {
            safe
        }
    }
}
