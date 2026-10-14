package io.github.deveshb1.peptideessentials.data

data class AppData(
    val medications: List<Medication> = emptyList(),
    val entries: List<DoseEntry> = emptyList(),
    val vials: List<Vial> = emptyList(),
    val customMetrics: List<Metric> = emptyList(),
    val measurements: List<Measurement> = emptyList(),
    val journal: List<JournalEntry> = emptyList(),
    val customSideEffects: List<String> = emptyList(),
    val settings: Settings = Settings(),
) {
    fun medication(id: String): Medication? = medications.firstOrNull { it.id == id }

    fun vial(id: String?): Vial? = id?.let { vialId -> vials.firstOrNull { it.id == vialId } }

    /** The vial doses are currently drawn from; the most recently mixed if several are open. */
    fun activeVial(medicationId: String): Vial? =
        vials.filter { it.medicationId == medicationId && it.status == VialStatus.ACTIVE }.maxByOrNull { it.mixedOn }

    val metrics: List<Metric> get() = BUILT_IN_METRICS + customMetrics

    fun metric(id: String): Metric? = metrics.firstOrNull { it.id == id }

    fun journalFor(date: java.time.LocalDate): JournalEntry? = journal.firstOrNull { it.date == date }

    companion object {
        val BUILT_IN_METRICS = listOf(
            Metric(Metric.WEIGHT_ID, "Weight", "kg", MetricKind.WEIGHT),
            Metric(Metric.WAIST_ID, "Waist", "cm", MetricKind.WAIST),
            Metric(Metric.BODY_FAT_ID, "Body fat", "%", MetricKind.BODY_FAT),
        )
    }
}
