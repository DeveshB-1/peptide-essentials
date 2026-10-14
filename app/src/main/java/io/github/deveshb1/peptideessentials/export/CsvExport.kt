package io.github.deveshb1.peptideessentials.export

import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.DoseStatus
import io.github.deveshb1.peptideessentials.data.SideEffect
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** CSV tables for each kind of data, newest first. Values use stored units (kg, cm) so files stay comparable. */
enum class CsvTable(val fileStem: String) { DOSES("doses"), VIALS("vials"), MEASUREMENTS("measurements"), JOURNAL("journal") }

object CsvExport {
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun build(table: CsvTable, data: AppData, zone: ZoneId): String = when (table) {
        CsvTable.DOSES -> doses(data, zone)
        CsvTable.VIALS -> vials(data)
        CsvTable.MEASUREMENTS -> measurements(data)
        CsvTable.JOURNAL -> journal(data)
    }

    fun isEmpty(table: CsvTable, data: AppData): Boolean = when (table) {
        CsvTable.DOSES -> data.entries.isEmpty()
        CsvTable.VIALS -> data.vials.isEmpty()
        CsvTable.MEASUREMENTS -> data.measurements.isEmpty()
        CsvTable.JOURNAL -> data.journal.isEmpty()
    }

    private fun doses(data: AppData, zone: ZoneId) = table(
        listOf("date", "time", "medication", "status", "amount", "unit", "site", "scheduled_date", "vial", "note"),
        data.entries.sortedByDescending { it.time }.map { e ->
            val local = e.time.atZone(zone)
            val taken = e.status == DoseStatus.TAKEN
            listOf(
                local.toLocalDate().toString(),
                if (taken) local.format(timeFormat) else "",
                e.medicationName,
                e.status.name.lowercase(),
                if (taken) number(e.amount) else "",
                e.unit.key,
                e.site?.name?.lowercase().orEmpty(),
                e.scheduledDate?.toString().orEmpty(),
                data.vial(e.vialId)?.label.orEmpty(),
                e.note,
            )
        },
    )

    private fun vials(data: AppData) = table(
        listOf("medication", "label", "amount", "unit", "diluent_ml", "concentration_per_ml", "mixed_on", "use_by", "status", "closed_on", "notes"),
        data.vials.sortedByDescending { it.mixedOn }.map { v ->
            listOf(
                data.medication(v.medicationId)?.name.orEmpty(),
                v.label,
                number(v.amount),
                v.unit.key,
                number(v.diluentMl),
                number(v.concentration),
                v.mixedOn.toString(),
                v.useBy.toString(),
                v.status.name.lowercase(),
                v.closedOn?.toString().orEmpty(),
                v.notes,
            )
        },
    )

    private fun measurements(data: AppData) = table(
        listOf("date", "metric", "value", "unit", "note"),
        data.measurements.sortedByDescending { it.date }.map { m ->
            val metric = data.metric(m.metricId)
            listOf(m.date.toString(), metric?.name ?: m.metricId, number(m.value), metric?.unitLabel.orEmpty(), m.note)
        },
    )

    private fun journal(data: AppData) = table(
        listOf("date", "energy", "sleep", "appetite", "mood", "side_effects", "reaction_site", "note"),
        data.journal.sortedByDescending { it.date }.map { j ->
            listOf(
                j.date.toString(),
                j.energy?.toString().orEmpty(),
                j.sleep?.toString().orEmpty(),
                j.appetite?.toString().orEmpty(),
                j.mood?.toString().orEmpty(),
                j.sideEffects.sorted().joinToString("; ") { key ->
                    SideEffect.fromKey(key)?.name?.lowercase() ?: key.removePrefix(SideEffect.CUSTOM_PREFIX)
                },
                j.reactionSite?.name?.lowercase().orEmpty(),
                j.note,
            )
        },
    )

    private fun number(value: Double) = value.toBigDecimal().stripTrailingZeros().toPlainString()

    private fun table(header: List<String>, rows: List<List<String>>) = buildString {
        appendRow(header)
        rows.forEach { appendRow(it) }
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
