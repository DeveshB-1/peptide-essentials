package io.github.deveshb1.peptideessentials.ui.nav

/** Routes for screens pushed above the tabs. Arguments follow a colon. */
object Route {
    const val MEDICATIONS = "medications"
    const val CALCULATOR = "calculator"
    const val SITES = "sites"
    const val HISTORY = "history"
    const val EXPORT = "export"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    fun editMedication(id: String?) = "medication:${id.orEmpty()}"
    fun editVial(id: String?, medicationId: String? = null) = "vial:${id.orEmpty()}:${medicationId.orEmpty()}"
    fun metric(id: String) = "metric:$id"
    fun journal(date: java.time.LocalDate) = "journal:$date"
    fun site(name: String) = "site:$name"

    /** Splits "name:arg1:arg2" into its parts; empty arguments become null. */
    fun parse(route: String): Pair<String, List<String?>> {
        val parts = route.split(":")
        return parts.first() to parts.drop(1).map { it.ifEmpty { null } }
    }
}
