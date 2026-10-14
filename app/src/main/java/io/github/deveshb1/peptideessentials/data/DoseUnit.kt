package io.github.deveshb1.peptideessentials.data

enum class DoseUnit(val key: String) {
    MG("mg"),
    MCG("mcg"),
    ML("mL"),
    UNITS("units"),
    IU("IU");

    companion object {
        fun fromKey(key: String): DoseUnit = entries.firstOrNull { it.key == key } ?: MG
    }
}
