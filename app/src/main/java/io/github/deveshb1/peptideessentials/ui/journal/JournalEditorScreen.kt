package io.github.deveshb1.peptideessentials.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.deveshb1.peptideessentials.R
import io.github.deveshb1.peptideessentials.data.AppData
import io.github.deveshb1.peptideessentials.data.InjectionSite
import io.github.deveshb1.peptideessentials.data.JournalEntry
import io.github.deveshb1.peptideessentials.data.SideEffect
import io.github.deveshb1.peptideessentials.ui.Format
import io.github.deveshb1.peptideessentials.ui.components.ChipChoice
import io.github.deveshb1.peptideessentials.ui.components.DateDialog
import io.github.deveshb1.peptideessentials.ui.components.PickerRow
import io.github.deveshb1.peptideessentials.ui.components.SectionHeader
import io.github.deveshb1.peptideessentials.ui.components.SubScreen
import io.github.deveshb1.peptideessentials.ui.nav.LocalNavigator
import io.github.deveshb1.peptideessentials.ui.nav.Route
import io.github.deveshb1.peptideessentials.ui.nav.rememberActions
import java.time.LocalDate

enum class Rating(val label: Int) {
    ENERGY(R.string.rating_energy),
    SLEEP(R.string.rating_sleep),
    APPETITE(R.string.rating_appetite),
    MOOD(R.string.rating_mood),
}

fun JournalEntry.rating(r: Rating): Int? = when (r) {
    Rating.ENERGY -> energy
    Rating.SLEEP -> sleep
    Rating.APPETITE -> appetite
    Rating.MOOD -> mood
}

private fun JournalEntry.withRating(r: Rating, value: Int?): JournalEntry = when (r) {
    Rating.ENERGY -> copy(energy = value)
    Rating.SLEEP -> copy(sleep = value)
    Rating.APPETITE -> copy(appetite = value)
    Rating.MOOD -> copy(mood = value)
}

/** Built-in side effects followed by the user's own, as stored keys. */
fun sideEffectKeys(data: AppData): List<String> =
    SideEffect.entries.map { it.name } + data.customSideEffects.map(SideEffect::customKey)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalEditorScreen(data: AppData, date: LocalDate) {
    val res = LocalResources.current
    val nav = LocalNavigator.current
    val actions = rememberActions()
    val today = LocalDate.now()
    val existing = data.journalFor(date)
    var entry by rememberSaveable(date) { mutableStateOf(JournalDraft.from(existing ?: JournalEntry(date))) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    val current = entry.toEntry()

    SubScreen(
        title = stringResource(R.string.journal_for, Format.relativeDay(res, date, today)),
        actions = {
            if (existing != null) {
                IconButton(onClick = {
                    actions.run { deleteJournal(date) }
                    actions.undoable(res.getString(R.string.entry_deleted), res.getString(R.string.undo)) { saveJournal(existing) }
                    nav.pop()
                }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
            }
            TextButton(onClick = {
                actions.run { saveJournal(current.copy(note = current.note.trim())) }
                nav.pop()
            }) { Text(stringResource(R.string.save)) }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PickerRow(
                icon = rememberVectorPainter(Icons.Filled.DateRange),
                label = stringResource(R.string.date),
                value = Format.longDate(date, today),
                onClick = { dialog = "date" },
            )

            SectionHeader(stringResource(R.string.ratings))
            Text(stringResource(R.string.ratings_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Rating.entries.forEach { r ->
                RatingRow(stringResource(r.label), current.rating(r)) { entry = JournalDraft.from(current.withRating(r, it)) }
            }

            SectionHeader(stringResource(R.string.side_effects))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sideEffectKeys(data).forEach { key ->
                    val selected = key in current.sideEffects
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val effects = if (selected) current.sideEffects - key else current.sideEffects + key
                            entry = JournalDraft.from(current.copy(sideEffects = effects))
                        },
                        label = { Text(Format.sideEffect(res, key)) },
                    )
                }
                AssistChip(
                    onClick = { dialog = "custom" },
                    label = { Text(stringResource(R.string.add_side_effect)) },
                    leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)) },
                )
            }
            if (current.hasReaction) {
                Text(stringResource(R.string.reaction_site_question), style = MaterialTheme.typography.bodyMedium)
                ChipChoice(InjectionSite.entries, current.reactionSite, { Format.site(res, it) }) {
                    val site = if (current.reactionSite == it) null else it
                    entry = JournalDraft.from(current.copy(reactionSite = site))
                }
            }

            OutlinedTextField(
                value = current.note,
                onValueChange = { entry = JournalDraft.from(current.copy(note = it)) },
                label = { Text(stringResource(R.string.journal_note)) },
                minLines = 4,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    when (dialog) {
        "date" -> DateDialog(
            initial = date,
            latest = today,
            onConfirm = {
                dialog = null
                if (it != date) nav.replace(Route.journal(it))
            },
            onDismiss = { dialog = null },
        )
        "custom" -> CustomSideEffectDialog(
            onDismiss = { dialog = null },
            onAdd = { label ->
                dialog = null
                actions.run { addCustomSideEffect(label) }
                entry = JournalDraft.from(current.copy(sideEffects = current.sideEffects + SideEffect.customKey(label)))
            },
        )
    }
}

@Composable
private fun RatingRow(label: String, value: Int?, onChange: (Int?) -> Unit) {
    val res = LocalResources.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        for (n in 1..5) {
            val selected = value != null && n <= value
            val description = res.getString(R.string.rating_description, label, n)
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .selectable(selected = value == n, role = Role.RadioButton) { onChange(if (value == n) null else n) }
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        n.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomSideEffectDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var label by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_side_effect)) },
        text = {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(30) },
                label = { Text(stringResource(R.string.side_effect_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = {
            TextButton(enabled = label.isNotBlank(), onClick = { onAdd(label.trim()) }) { Text(stringResource(R.string.add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
