package io.github.deveshb1.peptideessentials.timeline

import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.DoseEntry
import io.github.deveshb1.peptideessentials.data.JournalEntry
import io.github.deveshb1.peptideessentials.data.Measurement
import io.github.deveshb1.peptideessentials.data.Vial
import java.time.LocalDate
import java.time.ZoneId

sealed interface TimelineItem {
    data class Dose(val entry: DoseEntry) : TimelineItem
    data class VialMixed(val vial: Vial) : TimelineItem
    data class VialClosed(val vial: Vial) : TimelineItem
    data class Journal(val entry: JournalEntry) : TimelineItem
    data class Measured(val measurement: Measurement) : TimelineItem
}

data class TimelineDay(val date: LocalDate, val items: List<TimelineItem>)

object Timeline {
    /** Everything that happened, one group per day, newest day first. */
    fun build(data: AppData, zone: ZoneId): List<TimelineDay> {
        val items = buildList {
            data.entries.sortedBy { it.time }.forEach { add(it.displayDate(zone) to TimelineItem.Dose(it)) }
            data.vials.forEach { vial ->
                add(vial.mixedOn to TimelineItem.VialMixed(vial))
                vial.closedOn?.let { add(it to TimelineItem.VialClosed(vial)) }
            }
            data.measurements.forEach { add(it.date to TimelineItem.Measured(it)) }
            data.journal.forEach { add(it.date to TimelineItem.Journal(it)) }
        }
        return items.groupBy({ it.first }, { it.second })
            .toSortedMap(compareByDescending { it })
            .map { (date, dayItems) -> TimelineDay(date, dayItems) }
    }
}
