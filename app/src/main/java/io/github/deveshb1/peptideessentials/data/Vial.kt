package io.github.deveshb1.peptideessentials.data

import java.time.LocalDate

/** Units a vial's contents can be measured in. */
enum class VialUnit(val key: String) {
    MG("mg"),
    MCG("mcg"),
    IU("IU");

    companion object {
        fun fromKey(key: String): VialUnit = entries.firstOrNull { it.key == key } ?: MG
    }
}

enum class VialStatus { ACTIVE, FINISHED, DISCARDED }

/**
 * A vial of [amount] [unit] mixed with [diluentMl] of diluent.
 * What's left is derived from the doses logged against it, so edits to history stay consistent.
 */
data class Vial(
    val id: String,
    val medicationId: String,
    val amount: Double,
    val unit: VialUnit,
    val diluentMl: Double,
    val mixedOn: LocalDate,
    val useBy: LocalDate,
    val label: String = "",
    val notes: String = "",
    val status: VialStatus = VialStatus.ACTIVE,
    val closedOn: LocalDate? = null,
) {
    /** Amount per mL, in [unit]. */
    val concentration: Double get() = amount / diluentMl
}
