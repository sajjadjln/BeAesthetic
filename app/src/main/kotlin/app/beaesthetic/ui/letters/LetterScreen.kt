package app.beaesthetic.ui.letters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.beaesthetic.AppContainer
import app.beaesthetic.R
import app.beaesthetic.data.db.LetterEntity
import app.beaesthetic.domain.time.Chapters
import app.beaesthetic.domain.time.ProgramCalendar
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.components.Chip
import app.beaesthetic.ui.components.ChipKind
import app.beaesthetic.ui.components.PrimaryButton
import app.beaesthetic.ui.components.TopBar
import app.beaesthetic.ui.jalaliDayMonth
import app.beaesthetic.ui.theme.Ba
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * L2: a letter to the future self (D72). A plain text sheet; the opening date comes from the rules. After
 * sealing it can't be read or changed before its day.
 */
@Composable
fun LetterScreen(container: AppContainer, first: Boolean, onDone: () -> Unit) {
    val c = Ba.colors
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    val dates by produceState<Pair<LocalDate, LocalDate>?>(null) {
        val s = container.db.settings().get() ?: return@produceState
        val today = container.clock.programDate(s.rolloverHour)
        val calendar = ProgramCalendar(s.programStartDate, s.programEndDate, s.rolloverHour)
        value = today to Chapters.letterOpensOn(calendar, today, first)
    }
    val (writtenOn, opensOn) = dates ?: return

    Column(Modifier.fillMaxSize().background(c.bg).safeDrawingPadding().imePadding()) {
        TopBar(stringResource(R.string.letter_title), onDone, stringResource(R.string.cd_close), icon = BaIcons.X)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.letter_written_on, jalaliDayMonth(writtenOn)), ChipKind.NEUTRAL)
                Chip(stringResource(R.string.letter_opens_on, jalaliDayMonth(opensOn)), ChipKind.SPECIAL, icon = BaIcons.Lock)
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.letter_hint), style = Ba.type.body, color = c.text3) },
                textStyle = Ba.type.body.copy(color = c.text1),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                    focusedBorderColor = c.accent, unfocusedBorderColor = c.border, cursorColor = c.accent,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 200.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface2).padding(12.dp),
            ) {
                Icon(BaIcons.Lock, null, tint = c.text2, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.letter_seal_note, jalaliDayMonth(opensOn)), style = Ba.type.caption, color = c.text2)
            }
            PrimaryButton(
                stringResource(R.string.letter_seal),
                enabled = text.isNotBlank(),
                icon = BaIcons.Mail,
                onClick = {
                    scope.launch {
                        container.db.letters().insert(
                            LetterEntity(writtenOn = writtenOn, writtenAt = container.clock.now(), opensOn = opensOn, text = text.trim(), isFirst = first),
                        )
                        if (first) container.db.settings().get()?.let { container.db.settings().upsert(it.copy(firstLetterOfferDone = true)) }
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )
        }
    }
}
