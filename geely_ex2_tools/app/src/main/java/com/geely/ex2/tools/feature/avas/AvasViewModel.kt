package com.geely.ex2.tools.feature.avas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geely.ex2.tools.R
import com.geely.ex2.tools.data.avas.AvasConstants
import com.geely.ex2.tools.data.avas.AvasRepository
import com.geely.ex2.tools.data.avas.AvasSample
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

data class AvasUiState(
    val isMuted: Boolean = false,
    val isChanging: Boolean = false,
    val showMuteConfirm: Boolean = false,
    val isAvailable: Boolean = false,
    val isSupported: Boolean = false,
    val currentModeText: String = "",
    val statusText: String = "",
)

class AvasViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = AvasRepository(appContext)

    private val _uiState = MutableStateFlow(AvasUiState())
    val uiState: StateFlow<AvasUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var refreshJob: Job? = null

    fun onResume() {
        refreshState()
        startPolling()
    }

    fun onPause() {
        stopPolling()
    }

    fun onMuteSegmentSelected(index: Int) {
        if (_uiState.value.isChanging) return
        val wantMuted = index == 1
        if (wantMuted == _uiState.value.isMuted) return
        if (wantMuted) {
            _uiState.update { it.copy(showMuteConfirm = true) }
        } else {
            applyMute(false)
        }
    }

    fun onMuteConfirmDismiss() {
        _uiState.update { it.copy(showMuteConfirm = false) }
    }

    fun onMuteConfirmAccepted() {
        if (_uiState.value.isChanging) return
        _uiState.update { it.copy(showMuteConfirm = false) }
        applyMute(true)
    }

    private fun applyMute(muted: Boolean) {
        _uiState.update { it.copy(isChanging = true) }
        viewModelScope.launch {
            val write = withContext(Dispatchers.IO) {
                repository.setMuted(muted)
            }
            if (!write.ok) {
                val err = write.error.orEmpty()
                val needSystem = err.contains("Permission", ignoreCase = true) ||
                    err.contains("SecurityException", ignoreCase = true) ||
                    err.contains("CAR_CONTROL_AUDIO", ignoreCase = true)
                _uiState.update {
                    it.copy(
                        isChanging = false,
                        statusText = if (needSystem) {
                            appContext.getString(R.string.avas_status_need_system)
                        } else {
                            appContext.getString(
                                R.string.avas_status_write_error,
                                write.error ?: appContext.getString(R.string.avas_write_error_unknown),
                            )
                        },
                    )
                }
                if (!needSystem) {
                    refreshState()
                }
                return@launch
            }

            if (muted) {
                repository.startRestoreService("UI AVAS mute on")
            } else {
                repository.stopRestoreService("UI AVAS mute off")
            }
            refreshState()
        }
    }

    override fun onCleared() {
        stopPolling()
        refreshJob?.cancel()
        repository.close()
        super.onCleared()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(AvasConstants.AVAS_UI_POLL_INTERVAL_MS)
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
            val sample = withContext(Dispatchers.IO) {
                repository.readAvas()
            }
            val mutedSaved = repository.isMutedSaved()
            _uiState.update {
                buildUiState(sample, mutedSaved).copy(showMuteConfirm = it.showMuteConfirm)
            }
        }
    }

    private fun buildUiState(sample: AvasSample, mutedSaved: Boolean): AvasUiState {
        val muted = mutedSaved || (sample.isAvailable && sample.isMuted)
        return AvasUiState(
            isMuted = muted,
            isChanging = false,
            isAvailable = sample.isAvailable,
            isSupported = sample.isSupported,
            currentModeText = buildModeText(sample),
            statusText = buildStatusText(sample, mutedSaved),
        )
    }

    private fun buildModeText(sample: AvasSample): String {
        if (!sample.isAvailable) {
            return appContext.getString(R.string.avas_mode_unavailable)
        }
        return when (sample.mode) {
            0 -> appContext.getString(R.string.avas_mode_off)
            1 -> appContext.getString(R.string.avas_mode_type_1)
            2 -> appContext.getString(R.string.avas_mode_type_2)
            3 -> appContext.getString(R.string.avas_mode_type_3)
            else -> appContext.getString(R.string.avas_mode_unknown, sample.mode)
        }
    }

    private fun buildStatusText(sample: AvasSample, mutedSaved: Boolean): String {
        if (!sample.isAvailable) {
            return appContext.getString(R.string.avas_status_error, sample.source)
        }
        return if (mutedSaved) {
            appContext.getString(R.string.avas_status_ok_muted, sample.source)
        } else {
            appContext.getString(R.string.avas_status_ok, sample.source)
        }
    }
}
