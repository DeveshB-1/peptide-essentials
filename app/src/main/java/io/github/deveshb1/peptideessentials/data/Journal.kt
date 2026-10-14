package io.github.deveshb1.peptideessentials.data

import java.time.LocalDate

enum class SideEffect {
    NAUSEA, HEADACHE, FATIGUE, INJECTION_SITE_REACTION, GI_UPSET, INSOMNIA;

    companion object {
        const val CUSTOM_PREFIX = "custom:"

        fun fromKey(key: String): SideEffect? = entries.firstOrNull { it.name == key }
        fun customKey(label: String) = CUSTOM_PREFIX + label
    }
}

/** One entry per day. Ratings are 1–5 or null when not rated; side effects are [SideEffect] names or custom keys. */
data class JournalEntry(
    val date: LocalDate,
    val energy: Int? = null,
    val sleep: Int? = null,
    val appetite: Int? = null,
    val mood: Int? = null,
    val sideEffects: Set<String> = emptySet(),
    val reactionSite: InjectionSite? = null,
    val note: String = "",
) {
    val hasReaction: Boolean get() = SideEffect.INJECTION_SITE_REACTION.name in sideEffects
}
