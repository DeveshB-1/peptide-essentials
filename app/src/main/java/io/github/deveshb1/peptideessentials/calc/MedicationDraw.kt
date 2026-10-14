package io.github.deveshb1.peptideessentials.calc

import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.Medication

/** What to draw for [med]'s scheduled dose from its active vial, or null if there's no vial or no exact answer. */
fun AppData.drawFor(med: Medication): Draw? =
    activeVial(med.id)?.let { Dosing.draw(med.doseAmount, med.unit, it) }
