package io.github.deveshb1.peptideessentials.data

enum class WeightUnit { KG, LB }

enum class LengthUnit { CM, IN }

data class Settings(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val lengthUnit: LengthUnit = LengthUnit.CM,
    val useByDays: Int = DEFAULT_USE_BY_DAYS,
    val siteRestDays: Map<InjectionSite, Int> = emptyMap(),
    val dynamicColor: Boolean = false,
) {
    fun restDays(site: InjectionSite): Int = siteRestDays[site] ?: DEFAULT_REST_DAYS

    companion object {
        const val DEFAULT_USE_BY_DAYS = 28
        const val DEFAULT_REST_DAYS = 7
    }
}
