package io.github.deveshb1.peptideessentials.data

import java.time.LocalDate

enum class MetricKind { WEIGHT, WAIST, BODY_FAT, CUSTOM }

/** A tracked measurement type. Built-ins store canonical units (kg, cm, %); custom ones store what was typed. */
data class Metric(val id: String, val name: String, val unitLabel: String, val kind: MetricKind = MetricKind.CUSTOM) {
    companion object {
        const val WEIGHT_ID = "weight"
        const val WAIST_ID = "waist"
        const val BODY_FAT_ID = "body_fat"
    }
}

data class Measurement(
    val id: String,
    val metricId: String,
    val date: LocalDate,
    val value: Double,
    val note: String = "",
)
