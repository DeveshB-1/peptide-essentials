package io.github.deveshb1.peptideessentials.ui.journal

import io.github.deveshb1.peptideessentials.data.InjectionSite
import io.github.deveshb1.peptideessentials.data.JournalEntry
import java.io.Serializable
import java.time.LocalDate

/** Saveable copy of a journal entry being edited (survives rotation and process death). */
data class JournalDraft(
    val date: LocalDate,
    val ratings: List<Int?>,
    val sideEffects: List<String>,
    val reactionSite: InjectionSite?,
    val note: String,
) : Serializable {
    fun toEntry() = JournalEntry(
        date = date,
        energy = ratings[0],
        sleep = ratings[1],
        appetite = ratings[2],
        mood = ratings[3],
        sideEffects = sideEffects.toSet(),
        // Only keep the site while the reaction is ticked.
        reactionSite = reactionSite.takeIf { JournalEntry(date, sideEffects = sideEffects.toSet()).hasReaction },
        note = note,
    )

    companion object {
        private const val serialVersionUID = 1L

        fun from(e: JournalEntry) = JournalDraft(
            date = e.date,
            ratings = listOf(e.energy, e.sleep, e.appetite, e.mood),
            sideEffects = e.sideEffects.sorted(),
            reactionSite = e.reactionSite,
            note = e.note,
        )
    }
}
