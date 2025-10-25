package com.example.frigateviewer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.key
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.videolan.libvlc.LibVLC
import com.example.frigateviewer.data.model.Camera
import com.example.frigateviewer.data.model.ViewLayout
import com.example.frigateviewer.ui.components.MosaicGrid
import com.example.frigateviewer.ui.components.VideoPlayer
import com.example.frigateviewer.ui.viewmodel.CameraUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    uiState: CameraUiState,
    onOpenCameraSelector: () -> Unit,
    onLayoutChange: (ViewLayout) -> Unit,
    onRetry: () -> Unit,
    onToggleExpand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showLayoutMenu by remember { mutableStateOf(false) }

    // Full-screen content; system bars are hidden at the Activity level.
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }

                uiState.selectedCameras.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No cameras selected",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(onClick = onOpenCameraSelector) {
                            Text("Select Cameras")
                        }
                    }
                }

                else -> {
                    CameraGrid(
                        cameras = uiState.selectedCameras,
                        layout = uiState.viewLayout,
                        frigateHost = uiState.frigateHost,
                        expandedCameraIds = uiState.expandedCameraIds,
                        onToggleExpand = onToggleExpand,
                        onOpenMenu = onOpenCameraSelector
                    )
                }
        }
    }
}

@Composable
fun CameraGrid(
    cameras: List<Camera>,
    layout: ViewLayout,
    frigateHost: String,
    expandedCameraIds: Set<String>,
    onToggleExpand: (String) -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isMultiView = cameras.size > 1

    // One shared LibVLC instance for all tiles
    val libVLC = remember(isMultiView) {
        LibVLC(context, arrayListOf(
            "--network-caching=150",
            "--rtsp-tcp",
            "--live-caching=50",
            "--clock-jitter=0",
            "--clock-synchro=0",
            // Disable hardware decoding for multi-view to avoid decoder contention
            if (isMultiView) "--no-mediacodec" else "--codec=mediacodec_ndk,mediacodec_jni,all",
            "--no-mediacodec-dr", // Disable direct rendering to reduce overhead
            "--drop-late-frames",
            "--skip-frames",
            "--avcodec-fast", // Fast decoding mode
            "--avcodec-threads=2", // Limit threads per decoder
            "--file-caching=150",
            "-vv"
        ))
    }

    DisposableEffect(Unit) {
        onDispose { try { libVLC.release() } catch (_: Throwable) { } }
    }
    // Cache runtime-measured aspect ratios for this session
    val measuredAspectRatios = remember { mutableStateMapOf<String, Float>() }

    run {
        // Per-tile fit/fill overlay feedback
        val overlayTick = remember { mutableStateMapOf<String, Long>() }

        MosaicGrid(
            items = cameras,
            aspectRatio = { cam -> measuredAspectRatios[cam.id] ?: cam.aspectRatio ?: 16f / 9f },
            modifier = modifier.fillMaxSize()
        ) { camera, cellAspect ->
            val index = cameras.indexOf(camera)
            val audioForThisTile = index == 0
            val useSubStream = isMultiView
            val isFill = expandedCameraIds.contains(camera.id)
            key(camera.id) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .then(
                            Modifier.pointerInput(camera.id) {
                                detectTapGestures(
                                    onTap = {
                                        onOpenMenu()
                                    },
                                    onDoubleTap = {
                                        onToggleExpand(camera.id)
                                        overlayTick[camera.id] = System.currentTimeMillis()
                                    },
                                    onLongPress = {
                                        // reserved for future config menu
                                    }
                                )
                            }
                        )
                ) {
                    VideoPlayer(
                        libVLC = libVLC,
                        streamUrl = camera.getRtspUrl(frigateHost, useSubStream = useSubStream),
                        cameraName = camera.name,
                        enableAudio = audioForThisTile,
                        targetAspect = if (isFill) cellAspect else null,
                        onAspectRatio = { ar -> measuredAspectRatios[camera.id] = ar },
                        modifier = Modifier
                            .matchParentSize()
                    )

                    // Ephemeral in-frame overlay to indicate mode change
                    val tick = overlayTick[camera.id]
                    if (tick != null) {
                        var visible by remember(tick) { mutableStateOf(true) }
                        LaunchedEffect(tick) {
                            kotlinx.coroutines.delay(900)
                            visible = false
                            // Clear when hidden to avoid recomposition churn
                            overlayTick.remove(camera.id)
                        }
                        if (visible) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(Color.Black.copy(alpha = 0.5f),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isFill) "Aspect Fill" else "Aspect Fit",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
