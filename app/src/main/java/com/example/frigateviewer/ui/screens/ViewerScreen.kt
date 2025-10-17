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
    modifier: Modifier = Modifier
) {
    var showLayoutMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Frigate Viewer") },
                actions = {
                    // Camera selector
                    IconButton(onClick = onOpenCameraSelector) {
                        Icon(Icons.Default.Menu, contentDescription = "Select Cameras")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        frigateHost = uiState.frigateHost
                    )
                }
            }
        }
    }
}

@Composable
fun CameraGrid(
    cameras: List<Camera>,
    layout: ViewLayout,
    frigateHost: String,
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
    MosaicGrid(
        items = cameras,
        aspectRatio = { cam -> cam.aspectRatio ?: 16f / 9f },
        modifier = modifier.fillMaxSize()
    ) { camera ->
        val index = cameras.indexOf(camera)
        val audioForThisTile = index == 0
        val useSubStream = isMultiView
        key(camera.id) {
            VideoPlayer(
                libVLC = libVLC,
                streamUrl = camera.getRtspUrl(frigateHost, useSubStream = useSubStream),
                cameraName = camera.name,
                enableAudio = audioForThisTile,
                modifier = Modifier
            )
        }
    }
}
