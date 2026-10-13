package io.github.deveshb1.doselog.data

data class AppData(
    val medications: List<Medication> = emptyList(),
    val entries: List<DoseEntry> = emptyList(),
) {
    fun medication(id: String): Medication? = medications.firstOrNull { it.id == id }
}
