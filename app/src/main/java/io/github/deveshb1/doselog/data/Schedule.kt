package io.github.deveshb1.doselog.data

import java.time.DayOfWeek

sealed interface Schedule {
    data object Daily : Schedule

    data class Weekdays(val days: Set<DayOfWeek>) : Schedule

    /** Every [interval] days, counted from the medication's start date. */
    data class EveryNDays(val interval: Int) : Schedule {
        init {
            require(interval >= 1) { "interval must be at least 1" }
        }
    }
}
