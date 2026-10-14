package io.github.deveshb1.peptideessentials.schedule

import io.github.deveshb1.peptideessentials.data.DoseEntry
import io.github.deveshb1.peptideessentials.data.DoseStatus
import io.github.deveshb1.peptideessentials.data.DoseUnit
import io.github.deveshb1.peptideessentials.data.InjectionSite
import io.github.deveshb1.peptideessentials.data.Medication
import io.github.deveshb1.peptideessentials.data.Schedule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

fun med(
    schedule: Schedule,
    start: LocalDate = LocalDate.of(2026, 1, 1),
    time: LocalTime = LocalTime.of(8, 0),
    id: String = "m1",
    paused: Boolean = false,
    dose: Double = 1.0,
    unit: DoseUnit = DoseUnit.MG,
) = Medication(
    id = id,
    name = "Test",
    doseAmount = dose,
    unit = unit,
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
    amount: Double = 1.0,
    unit: DoseUnit = DoseUnit.MG,
    vialId: String? = null,
) = DoseEntry(
    id = "${medicationId}-$time-$site-$date",
    medicationId = medicationId,
    medicationName = "Test",
    scheduledDate = date,
    status = status,
    time = time,
    amount = amount,
    unit = unit,
    site = site,
    vialId = vialId,
)
