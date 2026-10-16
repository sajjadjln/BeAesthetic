package app.beaesthetic.ui.progress

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.beaesthetic.R
import app.beaesthetic.data.repo.PhotoStore
import app.beaesthetic.data.repo.Pose
import app.beaesthetic.domain.buddy.Outfit
import app.beaesthetic.domain.progress.DecimalInput
import app.beaesthetic.domain.progress.GuessOutcome
import app.beaesthetic.domain.progress.Progress
import app.beaesthetic.domain.progress.Reveals
import app.beaesthetic.domain.progress.Trend
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.domain.time.Jalali
import app.beaesthetic.ui.components.BaCard
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.components.Chip
import app.beaesthetic.ui.components.ChipKind
import app.beaesthetic.ui.components.InputBox
import app.beaesthetic.ui.components.PrimaryButton
import app.beaesthetic.ui.components.SecondaryButton
import app.beaesthetic.ui.components.TopBar
import app.beaesthetic.ui.components.buddy.Piker
import app.beaesthetic.ui.jalaliDayMonth
import app.beaesthetic.ui.theme.Ba
import app.beaesthetic.domain.buddy.Pose as BuddyPose

@Composable
fun CheckInRoute(viewModel: CheckInViewModel, photos: PhotoStore, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = Ba.colors
    val s = state
    BackHandler { if (!viewModel.back()) onClose() }
    Box(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding().imePadding()) {
        if (s == null) return@Box
        Column(Modifier.fillMaxSize()) {
            val months = stringArrayResource(R.array.jalali_months)
            val title = if (s.idx == 0) {
                stringResource(R.string.checkin_title_day0, jalaliDayMonth(s.date))
            } else {
                stringResource(R.string.checkin_title, months[Jalali.fromGregorian(s.date).month - 1])
            }
            TopBar(title, onClose, stringResource(R.string.cd_close), icon = BaIcons.X)
            if (s.step != CheckInStep.DONE) StepDots(s.steps.filter { it != CheckInStep.DONE }, s.step)
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                when (s.step) {
                    CheckInStep.WEIGHT -> WeightStep(s, viewModel)
                    CheckInStep.TAPE -> TapeStep(s, viewModel)
                    CheckInStep.TESTS -> TestsStep(s, viewModel)
                    CheckInStep.PHOTOS -> PhotosStep(s, photos, viewModel)
                    CheckInStep.DONE -> DoneStep(s, photos)
                }
            }
            val last = s.step == CheckInStep.DONE
            val waiting = s.step == CheckInStep.PHOTOS && s.savingPhoto
            if (waiting) {
                Text(stringResource(R.string.photo_saving), style = Ba.type.caption, color = c.text3, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            PrimaryButton(
                stringResource(if (last) R.string.checkin_finish else R.string.next),
                onClick = { if (last) onClose() else viewModel.next() },
                enabled = !waiting,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }
}

/** «وزن · متر · تست‌ها · عکس»: done steps in the accent, the current one bold. */
@Composable
private fun StepDots(steps: List<CheckInStep>, current: CheckInStep) {
    val c = Ba.colors
    val names = mapOf(
        CheckInStep.WEIGHT to R.string.step_weight,
        CheckInStep.TAPE to R.string.step_tape,
        CheckInStep.TESTS to R.string.step_tests,
        CheckInStep.PHOTOS to R.string.step_photos,
    )
    val at = steps.indexOf(current)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        steps.forEachIndexed { i, step ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (i <= at) c.accent else c.border))
                Text(stringResource(names.getValue(step)), style = Ba.type.caption, color = if (i == at) c.text1 else c.text3)
            }
        }
    }
}

@Composable
private fun WeightStep(s: CheckInUiState, vm: CheckInViewModel) {
    val c = Ba.colors
    val w = s.weight ?: return
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.weight_title), style = Ba.type.title, color = c.text1)
        Text(stringResource(R.string.weight_help), style = Ba.type.caption, color = c.text2)
        DecimalStepper(DecimalInput.parse(w.value) ?: w.last?.value ?: 70.0, w.metric.unit, onChange = vm::setWeight)
        w.last?.let { Text(stringResource(R.string.weight_prev, PersianDigits.format(it.value), jalaliDayMonth(it.date)), style = Ba.type.caption, color = c.text3) }
    }
}

@Composable
private fun TapeStep(s: CheckInUiState, vm: CheckInViewModel) {
    val c = Ba.colors
    Text(stringResource(R.string.tape_title, PersianDigits.format(s.tape.size)), style = Ba.type.title, color = c.text1)
    Text(stringResource(R.string.tape_help), style = Ba.type.caption, color = c.text2)
    BaCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            s.tape.forEach { item ->
                NumberRow(item, onChange = { vm.setValue(item.metric.id, it) })
            }
        }
    }
    if (s.tape.any { it.last != null }) Text(stringResource(R.string.tape_prefilled), style = Ba.type.caption, color = c.text3)
}

/** A metric's name, where to measure, the last value and a field. */
@Composable
private fun NumberRow(item: CheckInMetric, onChange: (String) -> Unit) {
    val c = Ba.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(item.metric.name, style = Ba.type.subtitle, color = c.text1)
            val hint = measureHint(item.metric.key)?.let { stringResource(it) }
            val prev = item.last?.let { stringResource(R.string.prev_value, PersianDigits.format(it.value)) }
            listOfNotNull(hint, prev).takeIf { it.isNotEmpty() }?.let { Text(it.joinToString(" · "), style = Ba.type.caption, color = c.text3) }
        }
        Box(Modifier.width(110.dp)) {
            InputBox(item.value, stringResource(unitName(item.metric.unit)), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal), onChange = onChange)
        }
    }
}

/** S1–S2 in one list: each test with its optional guess (pre-filled with the last result) and the result. */
@Composable
private fun TestsStep(s: CheckInUiState, vm: CheckInViewModel) {
    val c = Ba.colors
    Text(stringResource(R.string.tests_title), style = Ba.type.title, color = c.text1)
    Text(stringResource(R.string.tests_help), style = Ba.type.caption, color = c.text2)
    s.tests.forEach { item ->
        BaCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.metric.name, style = Ba.type.subtitle, color = c.text1, modifier = Modifier.weight(1f))
                    item.last?.let { Text(stringResource(R.string.prev_value, valueText(item.metric.unit, it.value)), style = Ba.type.caption, color = c.text3) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.test_guess) + " · " + stringResource(R.string.test_guess_help), style = Ba.type.caption, color = c.text2)
                        InputBox(item.guess, stringResource(unitName(item.metric.unit)), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal), onChange = { vm.setGuess(item.metric.id, it) })
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.test_result), style = Ba.type.caption, color = c.text2)
                        InputBox(item.value, stringResource(unitName(item.metric.unit)), keyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal), onChange = { vm.setValue(item.metric.id, it) })
                    }
                }
                val result = DecimalInput.parse(item.value)
                if (result != null) {
                    val guess = DecimalInput.parse(item.guess)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (Progress.guess(item.metric, guess, result)) {
                            GuessOutcome.BEAT -> Chip(stringResource(R.string.guess_beat), ChipKind.ACCENT)
                            GuessOutcome.SHOWN -> Chip(stringResource(R.string.guess_shown, PersianDigits.format(guess!!), PersianDigits.format(result)), ChipKind.NEUTRAL)
                            GuessOutcome.NONE -> Unit
                        }
                        if (Progress.isRecord(item.metric, result, item.earlier)) Chip(stringResource(R.string.progress_record), ChipKind.ACCENT)
                    }
                }
            }
        }
    }
}

/** Three photos with the consistency checklist (D64). The ghost-overlay camera comes in 7b. */
@Composable
private fun PhotosStep(s: CheckInUiState, photos: PhotoStore, vm: CheckInViewModel) {
    val c = Ba.colors
    val context = LocalContext.current
    var shooting by rememberSaveable { mutableStateOf<Pose?>(null) }
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var picking by rememberSaveable { mutableStateOf<Pose?>(null) }
    var ghosting by rememberSaveable { mutableStateOf<Pose?>(null) }
    val months = stringArrayResource(R.array.jalali_months)
    ghosting?.let { pose ->
        val ghost = s.ghost(pose)
        val label = ghost?.let { (gi, _) -> s.photoRows.first { it.checkIn.idx == gi }.checkIn.let { if (it.isDay0) stringResource(R.string.reveal_day0) else months[Jalali.fromGregorian(it.date).month - 1] } }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { ghosting = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            GhostCamera(photos, stringResource(poseName(pose)), ghost?.second, label, onShot = { uri -> vm.savePhoto(pose, uri); ghosting = null }, onClose = { ghosting = null })
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val pose = shooting
        val uri = cameraUri
        if (ok && pose != null && uri != null) vm.savePhoto(pose, uri)
        shooting = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val pose = picking
        if (uri != null && pose != null) vm.savePhoto(pose, uri)
        picking = null
    }

    Text(stringResource(R.string.photos_title), style = Ba.type.title, color = c.text1)
    Text(stringResource(R.string.photos_checklist), style = Ba.type.caption, color = c.onSpecialSubtle,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.specialSubtle).padding(12.dp))
    if (s.photoFailed) Text(stringResource(R.string.photo_failed), style = Ba.type.caption, color = c.amber)
    Pose.entries.forEach { pose ->
        BaCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val name = stringResource(poseName(pose))
                PhotoThumb(photos, s.photos[pose], stringResource(R.string.cd_photo, name, ""), Modifier.width(84.dp).aspectRatio(3f / 4f))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name, style = Ba.type.subtitle, color = c.text1)
                    // The ghost camera first (D64); the phone's camera and the gallery stay as fallbacks.
                    PrimaryButton(stringResource(R.string.photo_ghost_camera), icon = BaIcons.Camera, height = 40.dp, onClick = { ghosting = pose })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton(stringResource(R.string.photo_camera), height = 40.dp, onClick = {
                            val file = photos.cameraTarget()
                            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
                            shooting = pose
                            cameraUri = uri
                            camera.launch(uri)
                        })
                        SecondaryButton(stringResource(R.string.photo_gallery), icon = BaIcons.Image, height = 40.dp, onClick = {
                            picking = pose
                            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        })
                    }
                }
            }
        }
    }
}

/**
 * The end of a check-in: «پیکر» says wow, the changes since Day 0 (the right way in the accent, weight
 * neutral) and Day 0's photos next to today's. The reveal with a slider comes in 7b (D75).
 */
@Composable
private fun DoneStep(s: CheckInUiState, photos: PhotoStore) {
    val c = Ba.colors
    val hasHistory = s.all.any { (_, r) -> r.size > 1 }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Piker(if (s.idx == 0) BuddyPose.THUMBS else BuddyPose.WOW, c.accent, Outfit.NONE, Modifier.width(160.dp))
        val line = when {
            s.idx == 0 -> R.string.checkin_done_line_day0
            hasHistory -> R.string.checkin_done_line
            else -> R.string.checkin_done_line_first
        }
        Text(stringResource(line), style = Ba.type.subtitle, color = c.text1, textAlign = TextAlign.Center)
    }
    // The reveal (D75): this check-in's photos against Day 0 (or last month) with the slider.
    if (s.photos.isNotEmpty() && Reveals.against(s.idx, s.photoRows.map { it.checkIn.idx }) != null) {
        RevealContent(photos, s.photoRows, s.idx)
    }
    if (hasHistory) {
        BaCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.progress_since_day0), style = Ba.type.label, color = c.text2)
                s.all.forEach { (metric, readings) ->
                    val since = Progress.sinceDay0(metric, readings)?.takeIf { readings.size > 1 } ?: return@forEach
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(metric.name, style = Ba.type.body, color = c.text1, modifier = Modifier.weight(1f))
                        Text(valueText(metric.unit, since.latest.value), style = Ba.type.body, color = c.text2)
                        Chip(deltaText(since.delta), if (since.trend == Trend.BETTER) ChipKind.ACCENT else ChipKind.NEUTRAL)
                    }
                }
            }
        }
    }
}
