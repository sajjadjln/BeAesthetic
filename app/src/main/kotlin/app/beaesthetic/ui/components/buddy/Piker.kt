package app.beaesthetic.ui.components.buddy

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.beaesthetic.domain.buddy.Outfit
import app.beaesthetic.domain.buddy.Pose
import app.beaesthetic.ui.components.buddy.PikerRig.drawPiker
import app.beaesthetic.ui.components.buddy.PikerRig.inViewBox
import app.beaesthetic.ui.theme.BaMotion

/** «پیکر» in one pose, drawn in [color] (the accent). Decorative: the line next to him carries the meaning. */
@Composable
fun Piker(pose: Pose, color: Color, outfit: Outfit, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(PikerRig.VIEW_W / PikerRig.VIEW_H)) {
        inViewBox { drawPiker(pose, color, outfit) }
    }
}

/**
 * He changes pose only when something happens: a quick pop (0.88 → 1 with the bouncy spring) and a fade.
 * No idle motion (D84). The phone's "Remove animations" setting makes it instant.
 */
@Composable
fun AnimatedPiker(pose: Pose, color: Color, outfit: Outfit, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = pose,
        transitionSpec = {
            (scaleIn(BaMotion.bouncy(), initialScale = .88f) + fadeIn(tween(BaMotion.SHORT))) togetherWith fadeOut(tween(BaMotion.TAP))
        },
        label = "piker",
        modifier = modifier,
    ) { p -> Piker(p, color, outfit) }
}
