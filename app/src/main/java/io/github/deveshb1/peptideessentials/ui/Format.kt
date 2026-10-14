package io.github.deveshb1.peptideessentials.ui

import android.content.Context
import android.content.res.Resources
import android.text.format.DateFormat
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.calc.Draw
import io.github.deveshb1.peptideessentials.data.BodyArea
import io.github.deveshb1.peptideessentials.data.DoseUnit
import io.github.deveshb1.peptideessentials.data.InjectionSite
import io.github.deveshb1.peptideessentials.data.LengthUnit
import io.github.deveshb1.peptideessentials.data.Metric
import io.github.deveshb1.peptideessentials.data.MetricKind
import io.github.deveshb1.peptideessentials.data.Schedule
import io.github.deveshb1.peptideessentials.data.Settings
import io.github.deveshb1.peptideessentials.data.SideEffect
import io.github.deveshb1.peptideessentials.data.Side
import io.github.deveshb1.peptideessentials.data.Vial
import io.github.deveshb1.peptideessentials.data.VialUnit
import io.github.deveshb1.peptideessentials.data.WeightUnit
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** Locale-aware text for doses, schedules, sites, dates and times. */
object Format {
    private val locale: Locale get() = Locale.getDefault()

    fun amount(value: Double, maxDecimals: Int = 3): String = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = maxDecimals
        isGroupingUsed = false
    }.format(value)

    /** Parses a typed amount; a comma is accepted as the decimal separator. */
    fun parseAmount(text: String): Double? =
        text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it.isFinite() }

    fun dose(res: Resources, amount: Double, unit: DoseUnit): String {
        val number = amount(amount)
        return when (unit) {
            DoseUnit.UNITS -> {
                val whole = amount == Math.floor(amount) && amount < Int.MAX_VALUE
                // Fractional amounts take the "other" plural form in English.
                res.getQuantityString(R.plurals.dose_units, if (whole) amount.toInt() else 2, number)
            }
            else -> res.getString(R.string.dose_format, number, unitLabel(res, unit))
        }
    }

    fun unitLabel(res: Resources, unit: DoseUnit): String = res.getString(
        when (unit) {
            DoseUnit.MG -> R.string.unit_mg
            DoseUnit.MCG -> R.string.unit_mcg
            DoseUnit.ML -> R.string.unit_ml
            DoseUnit.UNITS -> R.string.unit_units
            DoseUnit.IU -> R.string.unit_iu
        },
    )

    fun area(res: Resources, area: BodyArea): String = res.getString(
        when (area) {
            BodyArea.ABDOMEN_UPPER -> R.string.area_abdomen_upper
            BodyArea.ABDOMEN_LOWER -> R.string.area_abdomen_lower
            BodyArea.THIGH -> R.string.area_thigh
            BodyArea.UPPER_ARM -> R.string.area_upper_arm
            BodyArea.GLUTE -> R.string.area_glute
        },
    )

    fun side(res: Resources, side: Side): String =
        res.getString(if (side == Side.LEFT) R.string.side_left else R.string.side_right)

    fun site(res: Resources, site: InjectionSite): String =
        res.getString(R.string.site_format, area(res, site.area), side(res, site.side))

    fun vialUnit(res: Resources, unit: VialUnit): String = res.getString(
        when (unit) {
            VialUnit.MG -> R.string.unit_mg
            VialUnit.MCG -> R.string.unit_mcg
            VialUnit.IU -> R.string.unit_iu
        },
    )

    fun vialAmount(res: Resources, amount: Double, unit: VialUnit): String =
        res.getString(R.string.dose_format, amount(amount), vialUnit(res, unit))

    fun concentration(res: Resources, vial: Vial): String =
        res.getString(R.string.concentration_format, amount(vial.concentration), vialUnit(res, vial.unit))

    /** "Draw 10 units (0.1 mL)". */
    fun draw(res: Resources, draw: Draw): String =
        res.getString(R.string.draw_format, amount(draw.units, 1), amount(draw.ml, 3))

    fun sideEffect(res: Resources, key: String): String = when (SideEffect.fromKey(key)) {
        SideEffect.NAUSEA -> res.getString(R.string.se_nausea)
        SideEffect.HEADACHE -> res.getString(R.string.se_headache)
        SideEffect.FATIGUE -> res.getString(R.string.se_fatigue)
        SideEffect.INJECTION_SITE_REACTION -> res.getString(R.string.se_site_reaction)
        SideEffect.GI_UPSET -> res.getString(R.string.se_gi_upset)
        SideEffect.INSOMNIA -> res.getString(R.string.se_insomnia)
        null -> key.removePrefix(SideEffect.CUSTOM_PREFIX)
    }

    fun metricName(res: Resources, metric: Metric): String = when (metric.kind) {
        MetricKind.WEIGHT -> res.getString(R.string.metric_weight)
        MetricKind.WAIST -> res.getString(R.string.metric_waist)
        MetricKind.BODY_FAT -> res.getString(R.string.metric_body_fat)
        MetricKind.CUSTOM -> metric.name
    }

    fun metricUnit(res: Resources, metric: Metric, settings: Settings): String = when (metric.kind) {
        MetricKind.WEIGHT -> res.getString(if (settings.weightUnit == WeightUnit.LB) R.string.unit_lb else R.string.unit_kg)
        MetricKind.WAIST -> res.getString(if (settings.lengthUnit == LengthUnit.IN) R.string.unit_in else R.string.unit_cm)
        MetricKind.BODY_FAT -> "%"
        MetricKind.CUSTOM -> metric.unitLabel
    }

    /** A display value with its unit, e.g. "82.4 kg" or "18 %". */
    fun metricValue(res: Resources, value: Double, metric: Metric, settings: Settings): String {
        val unit = metricUnit(res, metric, settings)
        return if (unit.isBlank()) amount(value, 1) else res.getString(R.string.dose_format, amount(value, 1), unit)
    }

    fun signed(value: Double, maxDecimals: Int = 1): String {
        val text = amount(kotlin.math.abs(value), maxDecimals)
        return when {
            text == amount(0.0, maxDecimals) -> text
            value > 0 -> "+$text"
            else -> "\u2212$text"
        }
    }

    fun schedule(res: Resources, schedule: Schedule): String = when (schedule) {
        Schedule.Daily -> res.getString(R.string.schedule_daily)
        is Schedule.Weekdays ->
            if (schedule.days.size == 7) {
                res.getString(R.string.schedule_daily)
            } else {
                orderedWeek()
                    .filter { it in schedule.days }
                    .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
            }
        is Schedule.EveryNDays ->
            res.getQuantityString(R.plurals.schedule_every_n_days, schedule.interval, schedule.interval)
    }

    /** Days of the week starting from the locale's first day. */
    fun orderedWeek(): List<java.time.DayOfWeek> {
        val first = WeekFields.of(locale).firstDayOfWeek
        return (0L until 7L).map { first.plus(it) }
    }

    fun time(context: Context, time: LocalTime): String {
        val skeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
        return time.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale))
    }

    fun shortDate(date: LocalDate): String = pattern(date, "EEEdMMM")

    fun longDate(date: LocalDate, today: LocalDate): String =
        pattern(date, if (date.year == today.year) "EEEEdMMMM" else "EEEEdMMMMyyyy")

    /** "Today", "Yesterday", "Tomorrow" or a short date. */
    fun relativeDay(res: Resources, date: LocalDate, today: LocalDate): String = when (date) {
        today -> res.getString(R.string.day_today)
        today.minusDays(1) -> res.getString(R.string.day_yesterday)
        today.plusDays(1) -> res.getString(R.string.day_tomorrow)
        else -> shortDate(date)
    }

    private fun pattern(date: LocalDate, skeleton: String): String =
        date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale))
}
