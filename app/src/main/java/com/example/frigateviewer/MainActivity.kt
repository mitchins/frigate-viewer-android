package com.example.frigateviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.frigateviewer.ui.screens.CameraSelectorSheet
import com.example.frigateviewer.ui.screens.ViewerScreen
import com.example.frigateviewer.ui.theme.FrigateViewerTheme
import com.example.frigateviewer.ui.viewmodel.CameraViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Hide system bars for immersive fullscreen viewing
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            FrigateViewerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FrigateViewerApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun FrigateViewerApp(viewModel: CameraViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showCameraSelector by remember { mutableStateOf(false) }

    ViewerScreen(
        uiState = uiState,
        onOpenCameraSelector = { showCameraSelector = true },
        onLayoutChange = { layout -> viewModel.setViewLayout(layout) },
        onRetry = { viewModel.loadCameras() },
        onToggleExpand = { id -> viewModel.toggleExpanded(id) },
        modifier = Modifier.fillMaxSize()
    )

    if (showCameraSelector) {
        CameraSelectorSheet(
            uiState = uiState,
            onApply = { selected -> if (selected.isNotEmpty()) viewModel.setSelectedCameras(selected) },
            onDismiss = { showCameraSelector = false }
        )
    }
}
