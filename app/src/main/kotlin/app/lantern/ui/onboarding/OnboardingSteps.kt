package app.lantern.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lantern.R
import app.lantern.ui.theme.Ba
import app.lantern.ui.theme.CharacterPortrait
import app.lantern.ui.theme.LanternCharacter
import app.lantern.ui.theme.Mood
import app.lantern.ui.theme.PixelDialog
import app.lantern.ui.theme.Px
import app.lantern.ui.theme.TodayLayout
import app.lantern.ui.theme.art

/** Each character's two lines when it's picked (character-pick.html): the hello, then a smile. */
private fun LanternCharacter.lines(): Pair<Int, Int> = when (this) {
    LanternCharacter.KNIGHT -> R.string.intro_knight_1 to R.string.intro_knight_2
    LanternCharacter.VAMPIRE -> R.string.intro_vampire_1 to R.string.intro_vampire_2
    LanternCharacter.WIZARD -> R.string.intro_wizard_1 to R.string.intro_wizard_2
    LanternCharacter.GOTH -> R.string.intro_goth_1 to R.string.intro_goth_2
}

/**
 * Step ①: the big portrait, its name tag, its hello in the dialog box, and the four as pixel cards (D186).
 * A tap picks (and the whole app re-themes at once, D187); tapping the picked one again makes it smile.
 */
@Composable
internal fun CharacterStep(picked: LanternCharacter, onPick: (LanternCharacter) -> Unit) {
    val c = Ba.colors
    var smile by rememberSaveable(picked) { mutableStateOf(false) }
    Text(stringResource(R.string.onb_char_title), style = Ba.type.headline, color = c.text1, textAlign = TextAlign.Center)
    Box(contentAlignment = Alignment.BottomCenter) {
        AnimatedContent(picked to smile, transitionSpec = { (fadeIn(tween(250)) + scaleIn(tween(250), 0.92f)) togetherWith fadeOut(tween(150)) }, label = "pick") { (who, happy) ->
            CharacterPortrait(who, if (happy) Mood.HAPPY else Mood.CALM, Modifier.size(196.dp))
        }
        Text(
            stringResource(picked.nameRes),
            style = Ba.type.label,
            color = c.onGlow,
            modifier = Modifier.background(c.glow).padding(horizontal = 11.dp, vertical = 5.dp),
        )
    }
    val (hello, second) = picked.lines()
    PixelDialog(stringResource(if (smile) second else hello), Modifier.fillMaxWidth().clickable { smile = !smile })
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        LanternCharacter.entries.forEach { who ->
            val on = who == picked
            val name = stringResource(who.nameRes)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(Px.Card)
                        .background(c.surface)
                        .border(if (on) 3.dp else 2.dp, if (on) c.glow else Color.White.copy(alpha = 0.12f), Px.Card)
                        .clickable(role = Role.RadioButton) { if (on) smile = !smile else onPick(who) }
                        .semantics { contentDescription = name; selected = on }
                        .padding(4.dp),
                ) {
                    Image(
                        painterResource(who.art(Mood.CALM)),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        // The ones not picked are dimmed and grayed (character-pick.html).
                        colorFilter = if (on) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.45f) }),
                        alpha = if (on) 1f else 0.7f,
                        modifier = Modifier.fillMaxSize().clip(Px.Small),
                    )
                }
                Text(name, style = Ba.type.caption, color = if (on) c.glow else c.text3, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

/** Step ②: Today's two layouts as pixel cards with a sketch each (D188); the character explains the picked one. */
@Composable
internal fun LayoutStep(character: LanternCharacter, picked: TodayLayout, onPick: (TodayLayout) -> Unit) {
    val c = Ba.colors
    Text(stringResource(R.string.onb_layout_title), style = Ba.type.headline, color = c.text1, textAlign = TextAlign.Center)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        CharacterPortrait(character, Mood.HAPPY, Modifier.size(76.dp))
        PixelDialog(
            stringResource(if (picked == TodayLayout.LIST) R.string.layout_list_line else R.string.layout_parts_line),
            Modifier.weight(1f),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        TodayLayout.entries.forEach { layout ->
            val on = layout == picked
            val title = stringResource(if (layout == TodayLayout.LIST) R.string.layout_list else R.string.layout_parts)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .clip(Px.Card)
                    .background(c.surface)
                    .border(if (on) 3.dp else 2.dp, if (on) c.glow else Color.White.copy(alpha = 0.12f), Px.Card)
                    .clickable(role = Role.RadioButton) { onPick(layout) }
                    .semantics { selected = on }
                    .padding(12.dp),
            ) {
                Text(title, style = Ba.type.subtitle, color = if (on) c.glow else c.text2)
                Sketch(layout, Modifier.fillMaxWidth().height(150.dp))
                Text(
                    stringResource(if (layout == TodayLayout.LIST) R.string.layout_list_help else R.string.layout_parts_help),
                    style = Ba.type.caption,
                    color = c.text3,
                )
            }
        }
    }
}

/** A tiny drawing of a Today layout: the «الان» card and rows, or the 4-part bar and rows. */
@Composable
private fun Sketch(layout: TodayLayout, modifier: Modifier) {
    val c = Ba.colors
    Canvas(modifier.background(Color.White.copy(alpha = 0.03f)).padding(8.dp)) {
        val row = 12.dp.toPx()
        val gap = 6.dp.toPx()
        var y = 0f
        if (layout == TodayLayout.LIST) {
            drawRect(c.glow.copy(alpha = 0.3f), Offset(0f, y), Size(size.width, 24.dp.toPx())); y += 24.dp.toPx() + gap
        } else {
            val w = (size.width - 3 * 3.dp.toPx()) / 4
            repeat(4) { i -> drawRect(if (i == 0) c.glow.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.1f), Offset(i * (w + 3.dp.toPx()), y), Size(w, 18.dp.toPx())) }
            y += 18.dp.toPx() + gap
        }
        var n = 0
        while (y + row <= size.height) {
            val header = layout == TodayLayout.LIST && n % 3 == 0
            drawRect(Color.White.copy(alpha = if (header) 0.2f else 0.1f), Offset(0f, y), Size(if (header) size.width * 0.4f else size.width, if (header) 4.dp.toPx() else row))
            y += (if (header) 4.dp.toPx() else row) + gap
            n++
        }
    }
}
