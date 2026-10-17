package app.beaesthetic.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.beaesthetic.R
import app.beaesthetic.domain.progress.Metric
import app.beaesthetic.domain.progress.MetricKind
import app.beaesthetic.domain.progress.MetricUnit
import app.beaesthetic.domain.progress.Suggestions
import app.beaesthetic.ui.components.BaCard
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.components.ErrorLine
import app.beaesthetic.ui.components.InputBox
import app.beaesthetic.ui.components.Labeled
import app.beaesthetic.ui.components.ListRow
import app.beaesthetic.ui.components.PickPill
import app.beaesthetic.ui.components.PrimaryButton
import app.beaesthetic.ui.components.SecondaryButton
import app.beaesthetic.ui.components.SectionTitle
import app.beaesthetic.ui.components.TopBar
import app.beaesthetic.ui.components.sheetScroll
import app.beaesthetic.ui.theme.Ba

private sealed interface MetricEditing {
    data object New : MetricEditing
    data class Edit(val metric: Metric) : MetricEditing
}

/** «اندازه‌ها و تست‌ها»: what's tracked, the suggestions not added yet (one tap, D100), and the user's own. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetricsRoute(viewModel: ProgressViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = Ba.colors
    var editing by remember { mutableStateOf<MetricEditing?>(null) }
    Box(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        val s = state ?: return@Box
        val metrics = listOfNotNull(s.weight) + s.others
        Column(Modifier.fillMaxSize()) {
            TopBar(stringResource(R.string.metrics_title), onBack, stringResource(R.string.cd_back))
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                MetricKind.entries.forEach { kind ->
                    val ofKind = metrics.filter { it.metric.kind == kind }
                    if (ofKind.isEmpty()) return@forEach
                    SectionTitle(stringResource(kindName(kind)))
                    BaCard(padding = PaddingValues(0.dp)) {
                        ofKind.forEach { card ->
                            ListRow(onClick = { editing = MetricEditing.Edit(card.metric) }) {
                                Column(Modifier.weight(1f)) {
                                    Text(card.metric.name, style = Ba.type.subtitle, color = c.text1)
                                    Text(
                                        stringResource(unitName(card.metric.unit)) + " · " +
                                            stringResource(if (card.metric.higherIsBetter) R.string.metric_higher else R.string.metric_lower),
                                        style = Ba.type.caption, color = c.text3,
                                    )
                                }
                                Icon(BaIcons.Pen, null, tint = c.text3, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                val remaining = Suggestions.remaining(metrics.map { it.metric.key })
                if (remaining.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.metrics_suggested))
                    Text(stringResource(R.string.metrics_suggested_help), style = Ba.type.caption, color = c.text3)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        remaining.forEach { suggested ->
                            val name = suggestionName(suggested.key)?.let { stringResource(it) } ?: suggested.key
                            PickPill("+ $name", selected = false) { viewModel.addSuggested(suggested, name) }
                        }
                    }
                }
                SecondaryButton(stringResource(R.string.metrics_add_own), onClick = { editing = MetricEditing.New }, icon = BaIcons.Plus, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    editing?.let { e ->
        MetricForm(
            editing = e,
            onDismiss = { editing = null },
            onSave = { kind, name, unit, higher ->
                viewModel.saveMetric((e as? MetricEditing.Edit)?.metric?.id, kind, name, unit, higher)
                editing = null
            },
            onRemove = { id -> viewModel.removeMetric(id); editing = null },
        )
    }
}

fun kindName(kind: MetricKind): Int = when (kind) {
    MetricKind.BODY -> R.string.metrics_kind_body
    MetricKind.TAPE -> R.string.metrics_kind_tape
    MetricKind.STRENGTH -> R.string.metrics_kind_strength
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MetricForm(
    editing: MetricEditing,
    onDismiss: () -> Unit,
    onSave: (MetricKind, String, MetricUnit, Boolean) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val c = Ba.colors
    val existing = (editing as? MetricEditing.Edit)?.metric
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var kind by remember { mutableStateOf(existing?.kind ?: MetricKind.STRENGTH) }
    var unit by remember { mutableStateOf(existing?.unit ?: MetricUnit.REPS) }
    var higher by remember { mutableStateOf(existing?.higherIsBetter ?: true) }
    var problem by remember { mutableIntStateOf(0) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().sheetScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Text(stringResource(if (existing == null) R.string.metric_form_new else R.string.metric_form_edit), style = Ba.type.title, color = c.text1)
            if (problem > 0) ErrorLine(R.string.metric_name_empty, problem)
            Labeled(R.string.metric_form_name) { InputBox(name, stringResource(R.string.metric_form_name_hint), onChange = { name = it.take(40) }) }
            Labeled(R.string.metric_form_kind) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricKind.entries.forEach { k -> PickPill(stringResource(kindName(k)), kind == k) { kind = k } }
                }
            }
            Labeled(R.string.metric_form_unit) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricUnit.entries.forEach { u -> PickPill(stringResource(unitName(u)), unit == u) { unit = u } }
                }
            }
            Labeled(R.string.metric_form_direction) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickPill(stringResource(R.string.metric_higher), higher) { higher = true }
                    PickPill(stringResource(R.string.metric_lower), !higher) { higher = false }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PrimaryButton(stringResource(R.string.save), modifier = Modifier.weight(1f), onClick = {
                    if (name.isBlank()) problem++ else onSave(kind, name, unit, higher)
                })
                if (existing != null) SecondaryButton(stringResource(R.string.delete), onClick = { onRemove(existing.id) })
            }
            if (existing != null) Text(stringResource(R.string.metric_remove_help), style = Ba.type.caption, color = c.text3)
        }
    }
}
