package io.github.deveshb1.peptideessentials.vials

import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.Vial
import io.github.deveshb1.peptideessentials.data.VialStatus
import java.time.LocalDate

sealed interface VialAlert {
    val vial: Vial

    /** Stable key so the same alert is only notified once. */
    val key: String

    data class Empty(override val vial: Vial) : VialAlert {
        override val key get() = "empty:${vial.id}"
    }

    data class Expired(override val vial: Vial) : VialAlert {
        override val key get() = "expired:${vial.id}"
    }

    data class LowStock(override val vial: Vial, val dosesLeft: Int) : VialAlert {
        override val key get() = "low:${vial.id}"
    }

    data class Expiring(override val vial: Vial, val days: Long) : VialAlert {
        override val key get() = "expiring:${vial.id}"
    }

    /** Empty or expired: the vial should be finished and maybe replaced. */
    val needsReplacing: Boolean get() = this is Empty || this is Expired

    companion object {
        /** The most pressing alert for each open vial of a medication that is still tracked. */
        fun forData(data: AppData, today: LocalDate): List<VialAlert> = data.vials
            .filter { it.status == VialStatus.ACTIVE }
            .mapNotNull { vial ->
                val med = data.medication(vial.medicationId) ?: return@mapNotNull null
                if (med.paused) return@mapNotNull null
                val state = VialMath.state(vial, med, data.entries, today)
                when {
                    state.empty -> Empty(vial)
                    state.expired -> Expired(vial)
                    state.lowStock -> LowStock(vial, state.dosesLeft!!)
                    state.expiring -> Expiring(vial, state.daysToUseBy)
                    else -> null
                }
            }
    }
}
