package com.example.frigateviewer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.frigateviewer.data.model.Camera
import com.example.frigateviewer.data.model.ViewLayout
import com.example.frigateviewer.data.repository.FrigateRepository
import com.example.frigateviewer.ui.model.SizingStrategy
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context

data class CameraUiState(
    val isLoading: Boolean = false,
    val allCameras: List<Camera> = emptyList(),
    val selectedCameras: List<Camera> = emptyList(),
    val viewLayout: ViewLayout = ViewLayout.SINGLE,
    val sizingStrategy: SizingStrategy = SizingStrategy.FIT,
    val expandedCameraIds: Set<String> = emptySet(),
    val error: String? = null,
    val frigateHost: String = "http://192.168.1.15:5000"
)

private val Context.dataStore by preferencesDataStore(name = "viewer_prefs")

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FrigateRepository()

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val PREF_SELECTED = stringPreferencesKey("selected_csv")
    private val PREF_EXPANDED = stringPreferencesKey("expanded_csv")

    init { loadCameras() }

    fun loadCameras() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.getCameras().fold(
                onSuccess = { cameras ->
                    // Apply persisted selection/expanded state against fetched cameras
                    val applied = applyPersistedState(cameras)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        allCameras = cameras,
                        selectedCameras = applied.first,
                        expandedCameraIds = applied.second,
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
        if (currentSelected.contains(camera)) {
            currentSelected.remove(camera)
        } else {
            currentSelected.add(camera)
        }
        // Remove expansion if deselected
        val newExpanded = _uiState.value.expandedCameraIds.filter { id -> currentSelected.any { it.id == id } }.toSet()
        _uiState.value = _uiState.value.copy(selectedCameras = currentSelected, expandedCameraIds = newExpanded)
        persistState()
    }

    fun setSelectedCameras(cameras: List<Camera>) {
        val expanded = _uiState.value.expandedCameraIds.filter { id -> cameras.any { it.id == id } }.toSet()
        _uiState.value = _uiState.value.copy(selectedCameras = cameras, expandedCameraIds = expanded)
        persistState()
    }

    fun setViewLayout(layout: ViewLayout) {
        _uiState.value = _uiState.value.copy(viewLayout = layout)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun updateFrigateHost(newHost: String) {
        repository.updateFrigateHost(newHost)
        _uiState.value = _uiState.value.copy(frigateHost = repository.getFrigateHost())
        loadCameras()
    }

    fun setSizingStrategy(strategy: SizingStrategy) {
        _uiState.value = _uiState.value.copy(sizingStrategy = strategy)
    }

    fun toggleExpanded(cameraId: String) {
        val current = _uiState.value.expandedCameraIds.toMutableSet()
        if (!current.add(cameraId)) current.remove(cameraId)
        // Only keep expansions for selected cameras
        val selectedIds = _uiState.value.selectedCameras.map { it.id }.toSet()
        val filtered = current.intersect(selectedIds)
        _uiState.value = _uiState.value.copy(expandedCameraIds = filtered)
        persistState()
    }

    private fun parseCsv(csv: String?): List<String> = csv?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
    private fun toCsv(items: Iterable<String>): String = items.joinToString(",")

    private fun applyPersistedState(all: List<Camera>): Pair<List<Camera>, Set<String>> {
        val ctx = getApplication<Application>().applicationContext
        // Blocking read is acceptable here since we're already on IO dispatcher in loadCameras
        // but to be safe, we will snapshot synchronously using runBlocking via first().
        // However, loadCameras already runs in viewModelScope.launch without specifying Dispatcher.
        // We will keep it light: if DataStore read fails, fallback.
        return try {
            val prefs = kotlinx.coroutines.runBlocking { ctx.dataStore.data.first() }
            val selCsv = prefs[PREF_SELECTED]
            val expCsv = prefs[PREF_EXPANDED]
            val selIds = parseCsv(selCsv)
            val expIds = parseCsv(expCsv).toSet()
            val byId = all.associateBy { it.id }
            val selected = selIds.mapNotNull { byId[it] }
            val finalSelected = if (selected.isNotEmpty()) selected else all.firstOrNull()?.let { listOf(it) } ?: emptyList()
            val finalExpanded = expIds.intersect(byId.keys)
            finalSelected to finalExpanded
        } catch (_: Throwable) {
            // Fallback: keep existing or auto-select first
            val fallbackSel = if (_uiState.value.selectedCameras.isNotEmpty()) _uiState.value.selectedCameras else all.firstOrNull()?.let { listOf(it) } ?: emptyList()
            fallbackSel to _uiState.value.expandedCameraIds.intersect(all.map { it.id }.toSet())
        }
    }

    private fun persistState() {
        val ctx = getApplication<Application>().applicationContext
        val selected = _uiState.value.selectedCameras.map { it.id }
        val expanded = _uiState.value.expandedCameraIds
        viewModelScope.launch {
            try {
                ctx.dataStore.edit { prefs ->
                    prefs[PREF_SELECTED] = toCsv(selected)
                    prefs[PREF_EXPANDED] = toCsv(expanded)
                }
            } catch (_: Throwable) { }
        }
    }
}
