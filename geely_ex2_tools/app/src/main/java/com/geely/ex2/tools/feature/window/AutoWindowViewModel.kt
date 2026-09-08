package com.geely.ex2.tools.feature.window

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geely.ex2.tools.data.window.AutoWindowRepository
import com.geely.ex2.tools.data.window.DoorWindowCornerState
import com.geely.ex2.tools.data.vhal.DoorCorner
import com.geely.ex2.tools.data.vhal.VhalConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AutoWindowUiState(
    /** Trạng thái bật/tắt riêng từng cửa: key = DoorCorner, value = enabled. */
    val cornerEnabled: Map<DoorCorner, Boolean> = DoorCorner.entries.associateWith { false },
    val corners: List<DoorWindowCornerState> = emptyList(),
    val isAvailable: Boolean = false,
)

class AutoWindowViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AutoWindowRepository(application)

    private val _uiState = MutableStateFlow(
        AutoWindowUiState(
            cornerEnabled = DoorCorner.entries.associateWith { repository.isCornerEnabled(it) },
        )
    )
    val uiState: StateFlow<AutoWindowUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var refreshJob: Job? = null

    fun onResume() {
        _uiState.update {
            it.copy(cornerEnabled = DoorCorner.entries.associateWith { c -> repository.isCornerEnabled(c) })
        }
        refreshState()
        startPolling()
    }

    fun onPause() {
        stopPolling()
    }

    fun onCornerEnabledChanged(corner: DoorCorner, enabled: Boolean) {
        if (enabled == _uiState.value.cornerEnabled[corner]) return
        _uiState.update { it.copy(cornerEnabled = it.cornerEnabled + (corner to enabled)) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.setCornerEnabled(corner, enabled, "UI auto window toggle ${corner.tag}")
            }
            refreshState()
        }
    }

    override fun onCleared() {
        stopPolling()
        refreshJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) { repository.close() }
        super.onCleared()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(VhalConstants.AUTO_WINDOW_UI_POLL_INTERVAL_MS)
                refreshState()
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun refreshState() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val corners = withContext(Dispatchers.IO) { repository.readCorners() }
            _uiState.update {
                it.copy(
                    corners = corners,
                    isAvailable = corners.any { corner ->
                        corner.isDoorOpen != null || corner.windowPercent != null
                    },
                )
            }
        }
    }
}
