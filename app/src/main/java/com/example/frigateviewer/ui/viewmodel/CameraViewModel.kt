package com.example.frigateviewer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frigateviewer.data.model.Camera
import com.example.frigateviewer.data.model.ViewLayout
import com.example.frigateviewer.data.repository.FrigateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CameraUiState(
    val isLoading: Boolean = false,
    val allCameras: List<Camera> = emptyList(),
    val selectedCameras: List<Camera> = emptyList(),
    val viewLayout: ViewLayout = ViewLayout.SINGLE,
    val error: String? = null,
    val frigateHost: String = "http://192.168.1.15:5000"
)

class CameraViewModel : ViewModel() {

    private val repository = FrigateRepository()

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        loadCameras()
    }

    fun loadCameras() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.getCameras().fold(
                onSuccess = { cameras ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        allCameras = cameras,
                        // Auto-select first camera if none selected
                        selectedCameras = if (_uiState.value.selectedCameras.isEmpty() && cameras.isNotEmpty()) {
                            listOf(cameras.first())
                        } else {
                            _uiState.value.selectedCameras
                        },
                        frigateHost = repository.getFrigateHost()
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unknown error occurred"
                    )
                }
            )
        }
    }

    fun toggleCameraSelection(camera: Camera) {
        val currentSelected = _uiState.value.selectedCameras.toMutableList()
        val maxCameras = _uiState.value.viewLayout.maxCameras

        if (currentSelected.contains(camera)) {
            currentSelected.remove(camera)
        } else {
            if (currentSelected.size < maxCameras) {
                currentSelected.add(camera)
            }
        }

        _uiState.value = _uiState.value.copy(selectedCameras = currentSelected)
    }

    fun setViewLayout(layout: ViewLayout) {
        val currentSelected = _uiState.value.selectedCameras

        // If we have more cameras selected than the layout allows, trim the list
        val adjustedSelection = if (currentSelected.size > layout.maxCameras) {
            currentSelected.take(layout.maxCameras)
        } else {
            currentSelected
        }

        _uiState.value = _uiState.value.copy(
            viewLayout = layout,
            selectedCameras = adjustedSelection
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun updateFrigateHost(newHost: String) {
        repository.updateFrigateHost(newHost)
        _uiState.value = _uiState.value.copy(frigateHost = repository.getFrigateHost())
        loadCameras()
    }
}
