package io.github.deveshb1.doselog.data

enum class BodyArea { ABDOMEN, THIGH, UPPER_ARM, BUTTOCK }

enum class Side { LEFT, RIGHT }

enum class InjectionSite(val area: BodyArea, val side: Side) {
    ABDOMEN_LEFT(BodyArea.ABDOMEN, Side.LEFT),
    ABDOMEN_RIGHT(BodyArea.ABDOMEN, Side.RIGHT),
    THIGH_LEFT(BodyArea.THIGH, Side.LEFT),
    THIGH_RIGHT(BodyArea.THIGH, Side.RIGHT),
    UPPER_ARM_LEFT(BodyArea.UPPER_ARM, Side.LEFT),
    UPPER_ARM_RIGHT(BodyArea.UPPER_ARM, Side.RIGHT),
    BUTTOCK_LEFT(BodyArea.BUTTOCK, Side.LEFT),
    BUTTOCK_RIGHT(BodyArea.BUTTOCK, Side.RIGHT);

    companion object {
        fun fromName(name: String?): InjectionSite? = entries.firstOrNull { it.name == name }
    }
}
