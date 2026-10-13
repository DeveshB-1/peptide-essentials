package io.github.deveshb1.doselog.schedule

import io.github.deveshb1.doselog.data.DoseEntry
import io.github.deveshb1.doselog.data.DoseStatus
import io.github.deveshb1.doselog.data.DoseUnit
import io.github.deveshb1.doselog.data.InjectionSite
import io.github.deveshb1.doselog.data.Medication
import io.github.deveshb1.doselog.data.Schedule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

fun med(
    schedule: Schedule,
    start: LocalDate = LocalDate.of(2026, 1, 1),
    time: LocalTime = LocalTime.of(8, 0),
    id: String = "m1",
    paused: Boolean = false,
) = Medication(
    id = id,
    name = "Test",
    doseAmount = 1.0,
    unit = DoseUnit.MG,
    schedule = schedule,
    timeOfDay = time,
    startDate = start,
    paused = paused,
)

fun entry(
    medicationId: String = "m1",
    date: LocalDate? = null,
    time: Instant = Instant.parse("2026-01-01T08:00:00Z"),
    status: DoseStatus = DoseStatus.TAKEN,
    site: InjectionSite? = null,
) = DoseEntry(
    id = "${medicationId}-$time-$site-$date",
    medicationId = medicationId,
    medicationName = "Test",
    scheduledDate = date,
    status = status,
    time = time,
    amount = 1.0,
    unit = DoseUnit.MG,
    site = site,
)
