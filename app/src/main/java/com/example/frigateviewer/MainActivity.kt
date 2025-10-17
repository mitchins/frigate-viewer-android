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
import com.example.frigateviewer.ui.screens.CameraSelectorSheet
import com.example.frigateviewer.ui.screens.ViewerScreen
import com.example.frigateviewer.ui.theme.FrigateViewerTheme
import com.example.frigateviewer.ui.viewmodel.CameraViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
        modifier = Modifier.fillMaxSize()
    )

    if (showCameraSelector) {
        CameraSelectorSheet(
            uiState = uiState,
            onCameraToggle = { camera -> viewModel.toggleCameraSelection(camera) },
            onDismiss = { showCameraSelector = false }
        )
    }
}
