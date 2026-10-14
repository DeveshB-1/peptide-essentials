package io.github.deveshb1.peptideessentials.data

enum class BodyView { FRONT, BACK }

enum class BodyArea(val view: BodyView) {
    ABDOMEN_UPPER(BodyView.FRONT),
    ABDOMEN_LOWER(BodyView.FRONT),
    THIGH(BodyView.FRONT),
    UPPER_ARM(BodyView.BACK),
    GLUTE(BodyView.BACK),
}

enum class Side { LEFT, RIGHT }

enum class InjectionSite(val area: BodyArea, val side: Side) {
    ABDOMEN_UPPER_LEFT(BodyArea.ABDOMEN_UPPER, Side.LEFT),
    ABDOMEN_UPPER_RIGHT(BodyArea.ABDOMEN_UPPER, Side.RIGHT),
    ABDOMEN_LOWER_LEFT(BodyArea.ABDOMEN_LOWER, Side.LEFT),
    ABDOMEN_LOWER_RIGHT(BodyArea.ABDOMEN_LOWER, Side.RIGHT),
    THIGH_LEFT(BodyArea.THIGH, Side.LEFT),
    THIGH_RIGHT(BodyArea.THIGH, Side.RIGHT),
    UPPER_ARM_LEFT(BodyArea.UPPER_ARM, Side.LEFT),
    UPPER_ARM_RIGHT(BodyArea.UPPER_ARM, Side.RIGHT),
    GLUTE_LEFT(BodyArea.GLUTE, Side.LEFT),
    GLUTE_RIGHT(BodyArea.GLUTE, Side.RIGHT);

    companion object {
        /** Version 1 had a single abdomen site per side and called glutes "buttock". */
        private val LEGACY = mapOf(
            "ABDOMEN_LEFT" to ABDOMEN_LOWER_LEFT,
            "ABDOMEN_RIGHT" to ABDOMEN_LOWER_RIGHT,
            "BUTTOCK_LEFT" to GLUTE_LEFT,
            "BUTTOCK_RIGHT" to GLUTE_RIGHT,
        )

        fun fromName(name: String?): InjectionSite? =
            name?.let { n -> entries.firstOrNull { it.name == n } ?: LEGACY[n] }
    }
}
