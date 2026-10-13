package io.github.deveshb1.doselog.ui

import android.content.Context
import android.content.res.Resources
import android.text.format.DateFormat
import io.github.deveshb1.doselog.R
import io.github.deveshb1.doselog.data.BodyArea
import io.github.deveshb1.doselog.data.DoseUnit
import io.github.deveshb1.doselog.data.InjectionSite
import io.github.deveshb1.doselog.data.Schedule
import io.github.deveshb1.doselog.data.Side
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

    fun amount(value: Double): String = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = 3
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
            BodyArea.ABDOMEN -> R.string.area_abdomen
            BodyArea.THIGH -> R.string.area_thigh
            BodyArea.UPPER_ARM -> R.string.area_upper_arm
            BodyArea.BUTTOCK -> R.string.area_buttock
        },
    )

    fun side(res: Resources, side: Side): String =
        res.getString(if (side == Side.LEFT) R.string.side_left else R.string.side_right)

    fun site(res: Resources, site: InjectionSite): String =
        res.getString(R.string.site_format, area(res, site.area), side(res, site.side))

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
