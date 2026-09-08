package com.geely.ex2.tools.feature.speed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geely.ex2.tools.R
import com.geely.ex2.tools.data.speed.SpeedRepository
import com.geely.ex2.tools.data.speed.SpeedWidgetRank
import com.geely.ex2.tools.data.vhal.SpeedSample
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class SpeedUiState(
    val isEnabled: Boolean = false,
    val widgetRank: Int = SpeedWidgetRank.DEFAULT,
    val canStepWidgetLeft: Boolean = true,
    val canStepWidgetRight: Boolean = true,
    val statusText: String = "",
    val latestSpeedText: String = "",
)

class SpeedViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = SpeedRepository(appContext)

    private val _uiState = MutableStateFlow(SpeedUiState())
    val uiState: StateFlow<SpeedUiState> = _uiState.asStateFlow()

    private var sampleJob: Job? = null
    private var resumeJob: Job? = null

    fun onResume() {
        resumeJob?.cancel()
        resumeJob = viewModelScope.launch {
            refreshSettings()
            if (!isActive) return@launch
            startObservingSample()
        }
    }

    fun onPause() {
        resumeJob?.cancel()
        resumeJob = null
        stopObservingSample()
    }

    fun onEnabledCheckedChange(enabled: Boolean) {
        if (enabled == _uiState.value.isEnabled) return
        repository.setEnabled(enabled)
        if (enabled) {
            syncBackgroundWork("UI speed enable")
            startObservingSample()
        } else {
            repository.stopStatusService("UI speed disable")
            repository.cancelStatusIcon()
            stopObservingSample()
        }
        refreshSettings()
    }

    fun onWidgetRankStep(delta: Int) {
        val currentRank = _uiState.value.widgetRank
        val canStep = if (delta < 0) {
            SpeedWidgetRank.canStepLeft(currentRank)
        } else {
            SpeedWidgetRank.canStepRight(currentRank)
        }
        if (!canStep) return

        val newRank = repository.stepStatusIconRank(delta)
        if (_uiState.value.isEnabled) {
            repository.notifyStatusIconIfEnabled("UI widget rank step", newRank)
        }
        _uiState.update {
            it.copy(
                widgetRank = newRank,
                canStepWidgetLeft = SpeedWidgetRank.canStepLeft(newRank),
                canStepWidgetRight = SpeedWidgetRank.canStepRight(newRank),
            )
        }
    }

    override fun onCleared() {
        stopObservingSample()
        super.onCleared()
    }

    private fun syncBackgroundWork(reason: String) {
        if (!repository.isEnabled()) {
            repository.stopStatusService("$reason, disabled")
            repository.cancelStatusIcon()
            return
        }
        repository.startStatusServiceIfEnabled(reason)
        repository.notifyStatusIconIfEnabled(reason)
    }

    private fun startObservingSample() {
        sampleJob?.cancel()
        if (!repository.isEnabled()) {
            applySample(null)
            return
        }
        sampleJob = viewModelScope.launch {
            repository.observeLatestSample().collect { sample ->
                if (!repository.isEnabled()) {
                    applySample(null)
                    return@collect
                }
                applySample(sample)
            }
        }
    }

    private fun stopObservingSample() {
        sampleJob?.cancel()
        sampleJob = null
    }

    private fun refreshSettings() {
        val enabled = repository.isEnabled()
        val widgetRank = repository.getStatusIconRank()
        val sample = if (enabled) repository.latestSample() else null
        _uiState.update {
            SpeedUiState(
                isEnabled = enabled,
                widgetRank = widgetRank,
                canStepWidgetLeft = SpeedWidgetRank.canStepLeft(widgetRank),
                canStepWidgetRight = SpeedWidgetRank.canStepRight(widgetRank),
                statusText = buildStatusText(enabled, sample),
                latestSpeedText = buildLatestSpeedText(enabled, sample),
            )
        }
    }

    private fun applySample(sample: SpeedSample?) {
        val enabled = repository.isEnabled()
        _uiState.update {
            it.copy(
                isEnabled = enabled,
                statusText = buildStatusText(enabled, sample.takeIf { enabled }),
                latestSpeedText = buildLatestSpeedText(enabled, sample.takeIf { enabled }),
            )
        }
    }

    private fun buildStatusText(enabled: Boolean, sample: SpeedSample?): String {
        if (!enabled) {
            return appContext.getString(R.string.speed_status_disabled)
        }
        if (sample == null) {
            return appContext.getString(R.string.speed_status_waiting)
        }
        return if (sample.isAvailable) {
            appContext.getString(R.string.speed_status_ok, sample.source)
        } else {
            appContext.getString(R.string.speed_status_error, sample.source)
        }
    }

    private fun buildLatestSpeedText(enabled: Boolean, sample: SpeedSample?): String {
        if (!enabled) {
            return appContext.getString(R.string.speed_latest_disabled)
        }
        if (sample?.isAvailable == true) {
            return appContext.getString(R.string.speed_latest_value, sample.speedKmh.roundToInt())
        }
        return appContext.getString(R.string.speed_latest_unavailable)
    }
}
