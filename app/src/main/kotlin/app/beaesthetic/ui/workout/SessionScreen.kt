package app.beaesthetic.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.beaesthetic.R
import app.beaesthetic.domain.buddy.Outfit
import app.beaesthetic.domain.buddy.Pose
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.workout.ExerciseKind
import app.beaesthetic.domain.workout.Offer
import app.beaesthetic.domain.workout.RoundNumbers
import app.beaesthetic.domain.workout.WorkoutRules
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.components.PrimaryButton
import app.beaesthetic.ui.components.SecondaryButton
import app.beaesthetic.ui.components.buddy.AnimatedPiker
import app.beaesthetic.ui.theme.Ba
import app.beaesthetic.ui.theme.BaMotion
import app.beaesthetic.ui.theme.rememberHaptics
import kotlinx.coroutines.delay

// ---------------------------------------------------------------- 04 Workout session

@Composable
fun SessionScreen(viewModel: SessionViewModel, onClose: () -> Unit, onOpenPlan: () -> Unit) {
    val c = Ba.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    // The «رکورد» moment (D54): which circle shows the chip right now.
    var flash by remember { mutableStateOf<Pair<Long, Int>?>(null) }
    LaunchedEffect(Unit) {
        viewModel.records.collect { key ->
            haptics.record()
            flash = key
            delay(RECORD_CHIP_MS)
            if (flash == key) flash = null
        }
    }

    Box(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding()) {
        when (val s = state) {
            SessionUi.Loading -> Unit
            is SessionUi.NoPlan -> NoPlan(s.outfit, onClose, onOpenPlan)
            is SessionUi.Logging -> Logging(s, flash, viewModel, onClose, onLog = { haptics.light(); viewModel.log() })
            is SessionUi.Summary -> Summary(s, onDone = onClose, onResume = viewModel::resume, onAccept = viewModel::acceptOffer, onLater = viewModel::laterOffer)
        }
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
    val c = Ba.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(c.surface)
            .clickable(role = Role.Button, onClick = onClose),
    ) { Icon(BaIcons.X, stringResource(R.string.cd_close), tint = c.text1, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun Logging(s: SessionUi.Logging, flash: Pair<Long, Int>?, vm: SessionViewModel, onClose: () -> Unit, onLog: () -> Unit) {
    val c = Ba.colors
    val item = s.item
    val seconds = item.exercise.kind == ExerciseKind.SECONDS
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // Top: close, one dash per move (tap to jump), and «پایان» once something is logged.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            CloseButton(onClose)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                modifier = Modifier.weight(1f),
            ) {
                s.exercisesDone.forEachIndexed { i, done ->
                    val label = stringResource(R.string.session_exercise_of, PersianDigits.format(i + 1), PersianDigits.format(s.exercisesDone.size))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(24.dp)
                            .clickable(role = Role.Tab) { vm.selectExercise(i) }
                            .semantics { contentDescription = label },
                    ) {
                        Box(
                            Modifier
                                .width(if (i == s.exerciseIndex) 30.dp else 20.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    when {
                                        done -> c.accent
                                        i == s.exerciseIndex -> c.text2
                                        else -> c.surface2
                                    },
                                ),
                        )
                    }
                }
            }
            if (s.anyLogged) {
                TextButton(onClick = vm::showSummary) { Text(stringResource(R.string.session_finish), style = Ba.type.label, color = c.text2) }
            } else {
                Spacer(Modifier.width(44.dp))
            }
        }

        Text(item.exercise.name, style = Ba.type.headline.copy(fontSize = 26.sp), color = c.text1, textAlign = TextAlign.Center)
        s.ladder?.let { l ->
            // «پله‌ی ۲ از ۴، بعدی: الماسی» (screens.html 04, D70).
            val rung = stringResource(R.string.ladder_rung, PersianDigits.format(l.rung), PersianDigits.format(l.of))
            val next = l.next?.let { stringResource(R.string.ladder_next, it) } ?: stringResource(R.string.ladder_top)
            Text(rung + SEP + next, style = Ba.type.label, color = c.onAccentSubtle, textAlign = TextAlign.Center)
        }
        val details = listOf(setsChip(item), planDetails(item)).filter { it.isNotEmpty() }.joinToString(SEP)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(details, style = Ba.type.label, color = c.text2, textAlign = TextAlign.Center)
            item.note?.let { Text(it, style = Ba.type.caption, color = c.text3, textAlign = TextAlign.Center) }
        }

        SetCircles(s, flash, vm::selectSet)

        // + on the right (first in RTL), the big number, − on the left (screens.html 04).
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            CounterButton(BaIcons.Plus, stringResource(R.string.cd_more)) { vm.stepValue(up = true) }
            Text(
                PersianDigits.format(s.draft.value),
                style = Ba.type.display.copy(fontSize = 96.sp, lineHeight = 100.sp),
                color = c.text1,
                textAlign = TextAlign.Center,
                // A fixed width keeps − and + under the thumb when the number grows to 3 digits (seconds).
                modifier = Modifier.width(170.dp),
            )
            CounterButton(BaIcons.Minus, stringResource(R.string.cd_less)) { vm.stepValue(up = false) }
        }
        val unit = stringResource(if (seconds) R.string.ex_kind_seconds else R.string.ex_kind_reps) +
            if (item.perSide) SEP + stringResource(R.string.ex_per_side) else ""
        Text(unit, style = Ba.type.label, color = c.text2)

        s.draft.kg?.let { kg -> KgChip(kg, onLess = { vm.stepKg(up = false) }, onMore = { vm.stepKg(up = true) }) }

        if (s.previous.isNotEmpty()) {
            val values = valuesLine(s.previous.map { it.value })
            val kg = s.previous.last().kg?.takeIf { item.kg != null }
            Text(
                stringResource(R.string.session_prev, values) + (kg?.let { SEP + kgText(it) } ?: ""),
                style = Ba.type.body,
                color = c.text3,
            )
        }

        EffortCard(s.draft.effort, vm::setEffort)

        val set = stringResource(R.string.session_set_of, PersianDigits.format(s.setIndex + 1), PersianDigits.format(item.sets))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(c.accent)
                .clickable(role = Role.Button, onClick = onLog),
        ) { Icon(BaIcons.Check, stringResource(R.string.session_log) + " · " + set, tint = c.onAccent, modifier = Modifier.size(36.dp)) }
        Text(set, style = Ba.type.caption, color = c.text3)
    }
}

/** Logged sets show their number; the current one follows the counter; a record fills with the accent (D54). */
@Composable
private fun SetCircles(s: SessionUi.Logging, flash: Pair<Long, Int>?, onSelect: (Int) -> Unit) {
    val c = Ba.colors
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
        s.circles.forEachIndexed { i, circle ->
            val flashing = flash == (s.item.exercise.id to i)
            val bg by animateColorAsState(
                when {
                    circle.record && circle.value != null && !circle.current -> c.accent
                    circle.value != null && !circle.current -> c.accentSubtle
                    else -> c.surface
                },
                tween(BaMotion.SHORT),
                label = "setCircle",
            )
            val fg = when {
                circle.record && !circle.current -> c.onAccent
                circle.value != null && !circle.current -> c.onAccentSubtle
                circle.current -> c.text1
                else -> c.text3
            }
            val burst = remember { Animatable(1f) }
            LaunchedEffect(flashing) {
                if (flashing) {
                    burst.snapTo(1.25f)
                    burst.animateTo(1f, BaMotion.bouncy())
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer { scaleX = burst.value; scaleY = burst.value }
                        .clip(CircleShape)
                        .background(bg)
                        .then(if (circle.current) Modifier.border(2.dp, c.accent, CircleShape) else Modifier)
                        .clickable(role = Role.Button) { onSelect(i) },
                ) {
                    Text(circle.value?.let { PersianDigits.format(it) } ?: "", style = Ba.type.subtitle, color = fg)
                }
                AnimatedVisibility(flashing, enter = scaleIn(BaMotion.bouncy()) + fadeIn(), exit = fadeOut()) {
                    Text(
                        stringResource(R.string.chip_record),
                        style = Ba.type.caption,
                        color = c.onAccent,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.accent).padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CounterButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    val c = Ba.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(c.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) { Icon(icon, null, tint = c.text1, modifier = Modifier.size(24.dp)) }
}

/** The small kg chip (D116): − / + in half kilos. */
@Composable
private fun KgChip(kg: Double, onLess: () -> Unit, onMore: () -> Unit) {
    val c = Ba.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.surface).padding(horizontal = 4.dp),
    ) {
        SmallStep(BaIcons.Minus, stringResource(R.string.cd_kg_less), enabled = kg > 0, onLess)
        Text(kgText(kg), style = Ba.type.subtitle, color = c.text1, modifier = Modifier.padding(horizontal = 4.dp))
        SmallStep(BaIcons.Plus, stringResource(R.string.cd_kg_more), enabled = kg < WorkoutRules.MAX_KG, onMore)
    }
}

@Composable
private fun SmallStep(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Ba.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) { Icon(icon, null, tint = if (enabled) c.text1 else c.text3, modifier = Modifier.size(18.dp)) }
}

/** «درجه سختی» 1–10 (D27). */
@Composable
private fun EffortCard(effort: Int, onChange: (Int) -> Unit) {
    val c = Ba.colors
    val label = stringResource(R.string.session_effort)
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = Ba.type.label, color = c.text2, modifier = Modifier.weight(1f))
            Text(PersianDigits.format(effort), style = Ba.type.title, color = c.onAccentSubtle)
        }
        Slider(
            value = effort.toFloat(),
            onValueChange = { onChange(Math.round(it)) },
            valueRange = WorkoutRules.MIN_EFFORT.toFloat()..WorkoutRules.MAX_EFFORT.toFloat(),
            steps = WorkoutRules.MAX_EFFORT - WorkoutRules.MIN_EFFORT - 1,
            colors = SliderDefaults.colors(
                thumbColor = c.text1,
                activeTrackColor = c.accent,
                inactiveTrackColor = c.surface2,
                activeTickColor = c.accent,
                inactiveTickColor = c.surface2,
            ),
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

@Composable
private fun NoPlan(outfit: Outfit, onClose: () -> Unit, onOpenPlan: () -> Unit) {
    val c = Ba.colors
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        CloseButton(onClose)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 96.dp, start = 16.dp, end = 16.dp),
        ) {
            AnimatedPiker(Pose.THINK, c.accent, outfit, Modifier.size(150.dp))
            Text(stringResource(R.string.session_no_plan_title), style = Ba.type.title, color = c.text1)
            Text(stringResource(R.string.session_no_plan_text), style = Ba.type.body, color = c.text2, textAlign = TextAlign.Center)
            PrimaryButton(stringResource(R.string.settings_workout), onClick = onOpenPlan, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** The end of a session: what was logged, the records, and «پیکر» cheering (D82). Round numbers come in 4b. */
@Composable
private fun Summary(s: SessionUi.Summary, onDone: () -> Unit, onResume: () -> Unit, onAccept: (Offer) -> Unit, onLater: (Offer) -> Unit) {
    val c = Ba.colors
    val lines = stringArrayResource(R.array.buddy_workout_done)
    val haptics = rememberHaptics()
    // A round number is a moment (D76): a medium buzz once, when the summary first shows it.
    LaunchedEffect(s.rounds.isNotEmpty()) { if (s.rounds.isNotEmpty()) haptics.medium() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        AnimatedPiker(Pose.CHEER, c.accent, s.outfit, Modifier.padding(top = 24.dp).size(170.dp))
        Text(lines[s.lineIndex % lines.size], style = Ba.type.subtitle, color = c.text2, textAlign = TextAlign.Center)
        Text(stringResource(if (s.allDone) R.string.summary_title else R.string.summary_partial), style = Ba.type.headline, color = c.text1)
        val counts = buildList {
            add(stringResource(R.string.summary_sets, PersianDigits.format(s.sets)))
            if (s.records > 0) add(stringResource(R.string.summary_records, PersianDigits.format(s.records)))
        }.joinToString(SEP)
        Text(counts, style = Ba.type.title, color = c.onAccentSubtle)
        s.rounds.forEach { RoundNumberLine(it) }
        s.offers.forEach { OfferCard(it, onAccept, onLater) }
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(16.dp),
        ) {
            s.lines.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(line.name, style = Ba.type.body, color = c.text1, modifier = Modifier.weight(1f))
                    val unit = if (line.kind == ExerciseKind.SECONDS) " " + stringResource(R.string.unit_seconds_short) else ""
                    Text(valuesLine(line.values) + unit, style = Ba.type.label, color = c.text2)
                }
            }
        }
        PrimaryButton(stringResource(R.string.session_finish), onClick = onDone, modifier = Modifier.fillMaxWidth())
        SecondaryButton(stringResource(R.string.summary_continue), onClick = onResume, modifier = Modifier.fillMaxWidth())
    }
}

private const val RECORD_CHIP_MS = 1_600L

/** «۱٬۰۰۰ تکرار شنا از روز صفر» (D121): lands with a bounce. */
@Composable
private fun RoundNumberLine(r: RoundUi) {
    val c = Ba.colors
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(r) { pop.animateTo(1f, BaMotion.bouncy()) }
    val n = r.crossing.value
    val text = when (r.crossing.kind) {
        RoundNumbers.Kind.REPS -> stringResource(R.string.round_reps, PersianDigits.grouped(n), r.name.orEmpty())
        RoundNumbers.Kind.SECONDS -> stringResource(R.string.round_minutes, PersianDigits.grouped(n / 60), r.name.orEmpty())
        RoundNumbers.Kind.WORKOUTS -> stringResource(R.string.round_workouts, PersianDigits.grouped(n))
        RoundNumbers.Kind.DAYS -> stringResource(R.string.round_days, PersianDigits.grouped(n))
    }
    Text(
        text,
        style = Ba.type.subtitle,
        color = c.onAccent,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .clip(RoundedCornerShape(50))
            .background(c.accent)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

/** «پله‌ی بعد: …؟» or «بازه بشه ۹–۱۳؟» with «بله» / «بعداً» (D120). Never changes the plan without «بله». */
@Composable
private fun OfferCard(o: OfferUi, onAccept: (Offer) -> Unit, onLater: (Offer) -> Unit) {
    val c = Ba.colors
    val (title, text) = when (val offer = o.offer) {
        is Offer.NextRung -> stringResource(R.string.offer_next_rung, offer.harder.name) to stringResource(R.string.offer_next_rung_text, o.moveName)
        is Offer.RaiseRange -> stringResource(R.string.offer_raise, PersianDigits.format(offer.from), PersianDigits.format(offer.to)) to
            stringResource(R.string.offer_raise_text, o.moveName)
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.accentSubtle).padding(16.dp),
    ) {
        Text(title, style = Ba.type.subtitle, color = c.onAccentSubtle)
        Text(text, style = Ba.type.caption, color = c.text2)
        if (o.accepted) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(BaIcons.Check, null, tint = c.onAccentSubtle, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.offer_accepted), style = Ba.type.label, color = c.onAccentSubtle)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryButton(stringResource(R.string.offer_yes), onClick = { onAccept(o.offer) }, height = 44.dp, modifier = Modifier.weight(1f))
                SecondaryButton(stringResource(R.string.later), onClick = { onLater(o.offer) }, height = 44.dp)
            }
        }
    }
}
