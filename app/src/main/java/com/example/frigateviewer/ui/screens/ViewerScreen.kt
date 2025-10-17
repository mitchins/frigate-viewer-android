package com.example.frigateviewer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.frigateviewer.data.model.Camera
import com.example.frigateviewer.data.model.ViewLayout
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
                    // Layout selector
                    IconButton(onClick = { showLayoutMenu = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Layout")
                    }
                    DropdownMenu(
                        expanded = showLayoutMenu,
                        onDismissRequest = { showLayoutMenu = false }
                    ) {
                        ViewLayout.entries.forEach { layout ->
                            DropdownMenuItem(
                                text = { Text(layout.displayName) },
                                onClick = {
                                    onLayoutChange(layout)
                                    showLayoutMenu = false
                                },
                                leadingIcon = {
                                    if (layout == uiState.viewLayout) {
                                        Text("✓")
                                    }
                                }
                            )
                        }
                    }

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
    LazyVerticalGrid(
        columns = GridCells.Fixed(layout.columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(cameras) { camera ->
            VideoPlayer(
                streamUrl = camera.getRtspUrl(frigateHost),
                cameraName = camera.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
        }
    }
}
