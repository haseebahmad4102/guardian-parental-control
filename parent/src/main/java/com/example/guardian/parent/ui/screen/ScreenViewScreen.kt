package com.example.guardian.parent.ui.screen

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.guardian.core.model.ScreenFrame

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenViewScreen(
    onBack: () -> Unit,
    viewModel: ScreenViewModel = hiltViewModel(),
) {
    val frame by viewModel.frame.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val live by viewModel.live.collectAsStateWithLifecycle()
    val commandStatus by viewModel.commandStatus.collectAsStateWithLifecycle()
    var showApps by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Screen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showApps = true }) {
                        Icon(Icons.Filled.Apps, contentDescription = "Open an app")
                    }
                    IconButton(onClick = viewModel::requestScreenshot) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FilterChip(
                selected = live,
                onClick = viewModel::toggleLive,
                label = { Text(if (live) "Live (auto-refresh on)" else "Live (auto-refresh off)") },
            )
            Spacer(Modifier.height(12.dp))

            ScreenImage(frame = frame, onTap = viewModel::tap)

            Spacer(Modifier.height(12.dp))
            commandStatus?.let {
                Text(
                    "Last command → $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (it.contains("failed")) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                if (frame == null)
                    "If \"pending\" stays: the child's background service was likely killed — open the " +
                        "Guardian app on the child and set it to No battery restrictions / Autostart. If " +
                        "\"failed\": read the reason above."
                else "Tap the image to tap the child's screen. Use the apps button to open an app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (showApps) {
        AlertDialog(
            onDismissRequest = { showApps = false },
            confirmButton = { TextButton(onClick = { showApps = false }) { Text("Close") } },
            title = { Text("Open an app") },
            text = {
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(apps, key = { it.pkg }) { app ->
                        OutlinedButton(
                            onClick = { viewModel.openApp(app.pkg); showApps = false },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Text(app.label.ifBlank { app.pkg })
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun ScreenImage(frame: ScreenFrame?, onTap: (Float, Float) -> Unit) {
    val bitmap: ImageBitmap? = remember(frame?.capturedAt) { frame?.let { decodeFrame(it.jpeg) } }

    if (bitmap == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.5f),
            contentAlignment = Alignment.Center,
        ) {
            Text("Requesting the screen…", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val aspect = if (frame != null && frame.height > 0) {
        frame.width.toFloat() / frame.height.toFloat()
    } else 0.5f

    Image(
        bitmap = bitmap,
        contentDescription = "Child screen",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .pointerInput(frame?.capturedAt) {
                detectTapGestures { offset ->
                    val fx = (offset.x / size.width).coerceIn(0f, 1f)
                    val fy = (offset.y / size.height).coerceIn(0f, 1f)
                    onTap(fx, fy)
                }
            },
    )
}

private fun decodeFrame(jpegBase64: String): ImageBitmap? = runCatching {
    val bytes = Base64.decode(jpegBase64, Base64.NO_WRAP)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}.getOrNull()
