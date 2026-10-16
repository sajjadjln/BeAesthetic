package app.beaesthetic.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.beaesthetic.R
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.workout.Exercise
import app.beaesthetic.domain.workout.ExerciseKind
import app.beaesthetic.domain.workout.PlanError
import app.beaesthetic.domain.workout.PlanForms
import app.beaesthetic.domain.workout.PlanItem
import app.beaesthetic.domain.workout.PlanItemForm
import app.beaesthetic.domain.workout.WorkoutPlan
import app.beaesthetic.domain.workout.WorkoutRules
import app.beaesthetic.ui.components.BaCard
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.components.BaSwitch
import app.beaesthetic.ui.components.Chip
import app.beaesthetic.ui.components.ChipKind
import app.beaesthetic.ui.components.ErrorLine
import app.beaesthetic.ui.components.InputBox
import app.beaesthetic.ui.components.Labeled
import app.beaesthetic.ui.components.ListRow
import app.beaesthetic.ui.components.PickPill
import app.beaesthetic.ui.components.PrimaryButton
import app.beaesthetic.ui.components.SecondaryButton
import app.beaesthetic.ui.components.StepButton
import app.beaesthetic.ui.components.Stepper
import app.beaesthetic.ui.components.TopBar
import app.beaesthetic.ui.settings.Segmented
import app.beaesthetic.ui.theme.Ba
import app.beaesthetic.ui.theme.PeykarTheme
import app.beaesthetic.ui.theme.ThemeMode
import kotlinx.coroutines.delay

private sealed interface EditingItem {
    data object New : EditingItem
    data class Existing(val item: PlanItem) : EditingItem
}

// ---------------------------------------------------------------- 10 Workout plan editor

@Composable
fun PlanRoute(viewModel: PlanViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlanScreen(
        state,
        onBack,
        PlanActions(viewModel::setName, viewModel::saveItem, viewModel::removeItem, viewModel::move),
    )
}

/** What the plan editor can change; each takes the weekday (Saturday = 0). */
data class PlanActions(
    val setName: (Int, String) -> Unit = { _, _ -> },
    val saveItem: (Int, PlanItemForm, PlanItem?) -> Unit = { _, _, _ -> },
    val removeItem: (Int, PlanItem) -> Unit = { _, _ -> },
    val move: (Int, Int, Int) -> Unit = { _, _, _ -> },
)

/** Stateless: the picked day and the open sheet are the only local UI state. */
@Composable
fun PlanScreen(state: PlanUiState?, onBack: () -> Unit, actions: PlanActions) {
    val c = Ba.colors
    var picked by rememberSaveable { mutableStateOf<Int?>(null) }
    var editing by remember { mutableStateOf<EditingItem?>(null) }

    Column(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        TopBar(stringResource(R.string.settings_workout), onBack, stringResource(R.string.cd_back))
        val s = state ?: return@Column
        val weekday = picked ?: s.todayWeekday
        val all = s.plans
        val exercises = s.exercises
        val links = s.links
        val plan = all[weekday]
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
        ) {
            DayPicker(all, weekday) { picked = it }
            WorkoutName(plan) { name -> actions.setName(plan.weekday, name) }
            if (plan.items.isEmpty()) {
                EmptyDay()
            } else {
                ExerciseList(plan, onOpen = { editing = EditingItem.Existing(it) }) { from, to -> actions.move(plan.weekday, from, to) }
                if (plan.items.size > 1) Text(stringResource(R.string.plan_drag_help), style = Ba.type.caption, color = c.text3, modifier = Modifier.padding(horizontal = 4.dp))
            }
            SecondaryButton(stringResource(R.string.plan_new_exercise), onClick = { editing = EditingItem.New }, icon = BaIcons.Plus, modifier = Modifier.fillMaxWidth())
        }

        editing?.let { e ->
            val original = (e as? EditingItem.Existing)?.item
            ExerciseSheet(
                original = original,
                dayItems = plan.items,
                exercises = exercises,
                links = links,
                onDismiss = { editing = null },
                onSave = { form ->
                    editing = null
                    actions.saveItem(plan.weekday, form, original)
                },
                onRemove = { item ->
                    editing = null
                    actions.removeItem(plan.weekday, item)
                },
            )
        }
    }
}

/** 7 days, Saturday first (on the right); each shows its number of moves, or «—» for a rest day. */
@Composable
private fun DayPicker(plans: List<WorkoutPlan>, selected: Int, onPick: (Int) -> Unit) {
    val c = Ba.colors
    val letters = stringArrayResource(R.array.weekdays_short)
    val names = stringArrayResource(R.array.weekdays)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        plans.forEach { plan ->
            val on = plan.weekday == selected
            val trains = plan.items.isNotEmpty()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) c.accentSubtle else c.surface)
                    .selectable(on, role = Role.Tab) { onPick(plan.weekday) }
                    .semantics { contentDescription = names[plan.weekday] }
                    .padding(vertical = 8.dp),
            ) {
                Text(letters[plan.weekday], style = Ba.type.subtitle, color = if (on) c.onAccentSubtle else c.text1)
                Text(
                    if (trains) PersianDigits.format(plan.items.size) else stringResource(R.string.none_dash),
                    style = Ba.type.caption,
                    color = when {
                        on -> c.onAccentSubtle
                        trains -> c.accent
                        else -> c.text3
                    },
                )
            }
        }
    }
}

/** Saved as the user types (like «ظاهر»), a moment after the last key. */
@Composable
private fun WorkoutName(plan: WorkoutPlan, onSave: (String) -> Unit) {
    val c = Ba.colors
    var text by remember(plan.weekday) { mutableStateOf(plan.name) }
    LaunchedEffect(plan.weekday, text) {
        if (text == plan.name) return@LaunchedEffect
        delay(400)
        onSave(text)
    }
    BasicTextField(
        value = text,
        onValueChange = { text = it.take(PlanForms.MAX_WORKOUT_NAME) },
        singleLine = true,
        textStyle = Ba.type.headline.copy(color = c.text1),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (text.isEmpty()) Text(stringResource(R.string.plan_name_hint), style = Ba.type.headline, color = c.text3)
                inner()
            }
        },
    )
}

@Composable
private fun EmptyDay() {
    val c = Ba.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
    ) {
        Icon(BaIcons.Dumbbell, null, tint = c.text3, modifier = Modifier.size(40.dp))
        Text(stringResource(R.string.plan_empty_title), style = Ba.type.title, color = c.text1)
        Text(stringResource(R.string.plan_empty_text), style = Ba.type.body, color = c.text2, textAlign = TextAlign.Center)
    }
}

/**
 * The day's moves with a drag handle each. The list is reordered locally while dragging and saved once on
 * release, so the database sees one move per drag.
 */
@Composable
private fun ExerciseList(plan: WorkoutPlan, onOpen: (PlanItem) -> Unit, onMove: (Int, Int) -> Unit) {
    val c = Ba.colors
    val rows = remember(plan) { mutableStateListOf<PlanItem>().apply { addAll(plan.items) } }
    val heights = remember(plan.weekday) { mutableStateMapOf<Long, Int>() }
    var dragged by remember(plan) { mutableStateOf<Long?>(null) }
    var startIndex by remember(plan) { mutableIntStateOf(-1) }
    var offset by remember(plan) { mutableFloatStateOf(0f) }

    BaCard(padding = PaddingValues(0.dp)) {
        rows.forEachIndexed { i, item ->
            val id = item.exercise.id
            val isDragged = dragged == id
            if (i > 0) HorizontalDivider(color = c.border)
            // Keyed, so a row that moves keeps its node and its drag gesture isn't cancelled mid-drag.
            key(id) {
                Box(
                    Modifier
                        .onSizeChanged { heights[id] = it.height }
                        .graphicsLayer {
                            translationY = if (isDragged) offset else 0f
                            shadowElevation = if (isDragged) 8f else 0f
                        }
                        .background(if (isDragged) c.surface2 else c.surface),
                ) {
                    ListRow(onClick = { onOpen(item) }) {
                        val dragLabel = stringResource(R.string.cd_drag)
                        Icon(
                            BaIcons.Grip,
                            null,
                            tint = c.text3,
                            modifier = Modifier
                                .size(22.dp)
                                .semantics { contentDescription = dragLabel }
                                .pointerInput(id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            dragged = id
                                            startIndex = rows.indexOfFirst { it.exercise.id == id }
                                            offset = 0f
                                        },
                                        onDragEnd = {
                                            val end = rows.indexOfFirst { it.exercise.id == id }
                                            if (end != startIndex && startIndex >= 0) onMove(startIndex, end)
                                            dragged = null
                                            offset = 0f
                                        },
                                        onDragCancel = { dragged = null; offset = 0f },
                                    ) { change, amount ->
                                        change.consume()
                                        offset += amount.y
                                        val at = rows.indexOfFirst { it.exercise.id == id }
                                        // Swap with a neighbour once the row is past half of it.
                                        if (offset > 0 && at < rows.lastIndex) {
                                            val h = heights[rows[at + 1].exercise.id] ?: return@detectDragGestures
                                            if (offset > h / 2f) { rows.add(at + 1, rows.removeAt(at)); offset -= h }
                                        } else if (offset < 0 && at > 0) {
                                            val h = heights[rows[at - 1].exercise.id] ?: return@detectDragGestures
                                            if (-offset > h / 2f) { rows.add(at - 1, rows.removeAt(at)); offset += h }
                                        }
                                    }
                                },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(item.exercise.name, style = Ba.type.subtitle, color = c.text1)
                            val details = planDetails(item)
                            if (details.isNotEmpty()) Text(details, style = Ba.type.caption, color = c.text3)
                        }
                        Chip(setsChip(item), ChipKind.NEUTRAL)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- exercise sheet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ExerciseSheet(
    original: PlanItem?,
    dayItems: List<PlanItem>,
    exercises: List<Exercise>,
    links: Map<Long, Long>,
    onDismiss: () -> Unit,
    onSave: (PlanItemForm) -> Unit,
    onRemove: (PlanItem) -> Unit,
) {
    val c = Ba.colors
    var form by remember(original) {
        val harder = original?.let { o -> links[o.exercise.id]?.let { id -> exercises.firstOrNull { it.id == id }?.name } }.orEmpty()
        mutableStateOf(original?.let { PlanForms.from(it, harder) } ?: PlanItemForm())
    }
    var tried by remember(original) { mutableStateOf(false) }
    val errors = PlanForms.errors(form, dayItems, original, exercises, links)
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scroll = rememberScrollState()
    LaunchedEffect(tried, errors.isEmpty()) { if (tried && errors.isNotEmpty()) scroll.animateScrollTo(0) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = c.surface) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(scroll).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Text(stringResource(if (original == null) R.string.ex_editor_new else R.string.ex_editor_edit), style = Ba.type.title, color = c.text1)

            Labeled(R.string.ex_name) {
                InputBox(form.name, stringResource(R.string.ex_name), onChange = { form = form.copy(name = it.take(PlanForms.MAX_NAME)) })
                if (tried && PlanError.NO_NAME in errors) ErrorLine(R.string.error_ex_name)
                if (tried && PlanError.DUPLICATE in errors) ErrorLine(R.string.error_ex_duplicate)
                // Moves already used on other days: one tap shares their history (D114).
                val suggestions = PlanForms.suggestions(form.name, exercises, dayItems.filter { it.exercise.id != original?.exercise?.id })
                if (suggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        suggestions.forEach { e ->
                            PickPill(e.name, selected = false, small = true) { form = PlanForms.withKind(form.copy(name = e.name), e.kind) }
                        }
                    }
                }
            }

            Labeled(R.string.ex_kind) {
                Segmented(
                    listOf(stringResource(R.string.ex_kind_reps), stringResource(R.string.ex_kind_seconds)),
                    form.kind.ordinal,
                ) { form = PlanForms.withKind(form, ExerciseKind.entries[it]) }
            }

            Labeled(R.string.ex_sets) {
                Stepper(form.sets, PlanForms.MIN_SETS..PlanForms.MAX_SETS, stringResource(R.string.ex_sets_value, PersianDigits.format(form.sets))) {
                    form = form.copy(sets = it)
                }
            }

            Labeled(R.string.ex_range) {
                SwitchRow(R.string.ex_max, R.string.ex_max_help, form.isMax) { form = form.copy(isMax = it) }
                if (!form.isMax) {
                    val step = WorkoutRules.step(form.kind)
                    val max = PlanForms.maxValue(form.kind)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Stepper(form.from, step..max, stringResource(R.string.ex_from, PersianDigits.format(form.from))) { v ->
                            form = PlanForms.withFrom(form, form.from + (v - form.from) * step)
                        }
                        Stepper(form.to, step..max, stringResource(R.string.ex_to, PersianDigits.format(form.to))) { v ->
                            form = PlanForms.withTo(form, form.to + (v - form.to) * step)
                        }
                    }
                }
            }

            SwitchRow(R.string.ex_kg, R.string.ex_kg_help, form.hasKg) { form = form.copy(hasKg = it) }
            if (form.hasKg) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StepButton(BaIcons.Minus, stringResource(R.string.cd_kg_less), enabled = form.kg > 0) { form = form.copy(kg = WorkoutRules.stepKg(form.kg, up = false)) }
                    Text(kgText(form.kg), style = Ba.type.subtitle, color = c.text1)
                    StepButton(BaIcons.Plus, stringResource(R.string.cd_kg_more), enabled = form.kg < WorkoutRules.MAX_KG) { form = form.copy(kg = WorkoutRules.stepKg(form.kg, up = true)) }
                }
            }

            SwitchRow(R.string.ex_per_side, R.string.ex_per_side_help, form.perSide) { form = form.copy(perSide = it) }

            Labeled(R.string.ex_effort) {
                val label = if (form.effort == 0) stringResource(R.string.none_dash) else PersianDigits.format(form.effort)
                Stepper(form.effort, 0..WorkoutRules.MAX_EFFORT, label) { form = form.copy(effort = it) }
            }

            Labeled(R.string.ex_rest) {
                val label = if (form.rest == 0) stringResource(R.string.none_dash) else restText(form.rest)
                Stepper(form.rest / PlanForms.REST_STEP, 0..PlanForms.MAX_REST / PlanForms.REST_STEP, label) { form = form.copy(rest = it * PlanForms.REST_STEP) }
            }

            // «حرکت سخت‌تر» (D119): the next rung of this move's ladder.
            Labeled(R.string.ex_harder) {
                InputBox(form.harder, stringResource(R.string.ex_harder_hint), onChange = { form = form.copy(harder = it.take(PlanForms.MAX_NAME)) })
                if (PlanError.HARDER_LOOP in errors) ErrorLine(R.string.error_harder_loop)
                val harderSuggestions = PlanForms.harderSuggestions(form.harder, form.name, exercises, links)
                if (harderSuggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        harderSuggestions.forEach { e -> PickPill(e.name, selected = false, small = true) { form = form.copy(harder = e.name) } }
                    }
                }
                Text(stringResource(R.string.ex_harder_help), style = Ba.type.caption, color = c.text3)
            }

            Labeled(R.string.ex_note) {
                InputBox(form.note, stringResource(R.string.ex_note_hint), onChange = { form = form.copy(note = it.take(PlanForms.MAX_NOTE)) })
            }

            PrimaryButton(
                stringResource(R.string.save),
                onClick = { if (errors.isEmpty()) onSave(form) else tried = true },
                modifier = Modifier.fillMaxWidth(),
            )
            if (original != null) {
                // Taking a move out of a day keeps its history; adding it back brings it all back.
                TextButton(onClick = { onRemove(original) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.ex_remove), style = Ba.type.label, color = c.text2)
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: Int, help: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Ba.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = Ba.type.subtitle, color = c.text1)
            Text(stringResource(help), style = Ba.type.caption, color = c.text3)
        }
        BaSwitch(checked, onChange, stringResource(title))
    }
}
