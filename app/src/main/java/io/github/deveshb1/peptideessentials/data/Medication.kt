package io.github.deveshb1.peptideessentials.data

import java.time.LocalDate
import java.time.LocalTime

data class Medication(
    val id: String,
    val name: String,
    val doseAmount: Double,
    val unit: DoseUnit,
    val schedule: Schedule,
    val timeOfDay: LocalTime,
    val startDate: LocalDate,
    val notes: String = "",
    val colorIndex: Int = 0,
    val usesInjectionSites: Boolean = false,
    val remindersEnabled: Boolean = true,
    val paused: Boolean = false,
    /** Doses before this date are never reported as missed: set when added and when resumed. */
    val trackedFrom: LocalDate = startDate,
)
