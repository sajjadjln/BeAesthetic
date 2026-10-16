package app.beaesthetic.ui.progress

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.beaesthetic.R
import app.beaesthetic.data.repo.PhotoStore
import app.beaesthetic.domain.text.PersianDigits
import app.beaesthetic.ui.components.BaIcons
import app.beaesthetic.ui.theme.Ba
import kotlinx.coroutines.delay
import java.io.File

/**
 * The ghost-overlay camera (D64, flows C4): last month's photo for this pose sits faint over the preview,
 * for framing only; a 10 s timer for a phone on a stand; the consistency checklist stays on screen.
 * The shot goes to a cache file, then through [PhotoStore.import] like any other photo.
 */
@Composable
fun GhostCamera(
    photos: PhotoStore,
    poseName: String,
    ghost: String?,
    ghostLabel: String?,
    onShot: (Uri) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val c = Ba.colors
    val shootLabel = stringResource(R.string.camera_shoot)
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.CAMERA) }
    BackHandler(onBack = onClose)

    var front by remember { mutableStateOf(false) }
    var timer by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val capture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build() }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FIT_CENTER } }

    if (granted) {
        DisposableEffect(front) {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycle, selector, preview, capture)
                }
            }, ContextCompat.getMainExecutor(context))
            onDispose { runCatching { future.get().unbindAll() } }
        }
    }

    fun shoot() {
        val file = File(photos.cameraTarget().path)
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    busy = false
                    onShot(Uri.fromFile(file))
                }

                override fun onError(exception: ImageCaptureException) {
                    busy = false
                }
            },
        )
    }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
            if (countdown == 0) shoot()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClose),
                ) { Icon(BaIcons.X, stringResource(R.string.cd_close), tint = Color.White) }
                Text(poseName, style = Ba.type.subtitle, color = Color.White, modifier = Modifier.weight(1f))
                ghostLabel?.let { Text(stringResource(R.string.camera_ghost, it), style = Ba.type.caption, color = Color.White.copy(alpha = 0.7f)) }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (granted) {
                    Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
                        AndroidView({ previewView }, Modifier.fillMaxSize())
                        // The ghost: last month's photo, faint and mirrored with the front camera, for framing only.
                        if (ghost != null) {
                            PhotoThumb(
                                photos, ghost, "",
                                Modifier.fillMaxSize().alpha(0.32f).graphicsLayer { if (front) scaleX = -1f },
                                maxEdge = 1080,
                            )
                        }
                    }
                    if (countdown > 0) Text(PersianDigits.format(countdown), style = Ba.type.display, color = Color.White)
                } else {
                    Text(stringResource(R.string.camera_no_permission), style = Ba.type.body, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
                }
            }
            Text(
                stringResource(if (ghost != null) R.string.camera_match else R.string.photos_checklist),
                style = Ba.type.caption, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            )
            if (ghost != null) {
                Text(stringResource(R.string.photos_checklist), style = Ba.type.caption, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            ) {
                Toggle(stringResource(R.string.camera_timer), timer) { timer = !timer }
                Box(
                    Modifier.size(76.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape).padding(8.dp).clip(CircleShape)
                        .background(if (busy || countdown > 0) c.accent else Color.White)
                        .clickable(enabled = granted && !busy && countdown == 0, role = Role.Button) {
                            busy = true
                            if (timer) countdown = 10 else shoot()
                        }
                        .semantics { contentDescription = shootLabel },
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).clickable(role = Role.Button) { front = !front },
                ) { Icon(BaIcons.Repeat, stringResource(R.string.camera_flip), tint = Color.White) }
            }
        }
    }
}

@Composable
private fun Toggle(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(24.dp))
            .background(if (on) Ba.colors.accent else Color.White.copy(alpha = 0.15f)).clickable(role = Role.Switch, onClick = onClick),
    ) { Text(text, style = Ba.type.label, color = Color.White) }
}
