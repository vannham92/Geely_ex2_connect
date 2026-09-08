package com.geely.ex2.tools.feature.temperature

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geely.ex2.tools.R
import com.geely.ex2.tools.data.temperature.TemperatureReader
import com.geely.ex2.tools.data.temperature.TemperatureRepository
import com.geely.ex2.tools.data.temperature.TemperatureWidgetRank
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
import kotlin.math.roundToInt

data class TemperatureUiState(
    val isEnabled: Boolean = true,
    val widgetRank: Int = TemperatureWidgetRank.DEFAULT,
    val canStepWidgetLeft: Boolean = true,
    val canStepWidgetRight: Boolean = true,
    val statusText: String = "",
    val latestTemperatureText: String = "",
    val temperatureC: Int? = null,
)

class TemperatureViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = TemperatureRepository(appContext)

    private val _uiState = MutableStateFlow(TemperatureUiState())
    val uiState: StateFlow<TemperatureUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var refreshJob: Job? = null

    fun onResume() {
        refreshState()
        restartPolling()
    }

    fun onPause() {
        stopPolling()
    }

    fun onEnabledCheckedChange(enabled: Boolean) {
        if (enabled == _uiState.value.isEnabled) return
        repository.setEnabled(enabled)
        // The toggle only controls the status-bar icon/service; the in-app value always shows.
        if (enabled) {
            syncBackgroundWork("UI temperature enable")
        } else {
            repository.stopStatusService("UI temperature disable")
            repository.cancelStatusIcon()
        }
        refreshState()
    }

    fun onWidgetRankStep(delta: Int) {
        val currentRank = _uiState.value.widgetRank
        val canStep = if (delta < 0) {
            TemperatureWidgetRank.canStepLeft(currentRank)
        } else {
            TemperatureWidgetRank.canStepRight(currentRank)
        }
        if (!canStep) return

        val newRank = repository.stepStatusIconRank(delta)
        if (_uiState.value.isEnabled) {
            repository.notifyStatusIconIfEnabled("UI widget rank step", newRank)
        }
        _uiState.update {
            it.copy(
                widgetRank = newRank,
                canStepWidgetLeft = TemperatureWidgetRank.canStepLeft(newRank),
                canStepWidgetRight = TemperatureWidgetRank.canStepRight(newRank),
            )
        }
    }

    override fun onCleared() {
        stopPolling()
        refreshJob?.cancel()
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

    private fun restartPolling() {
        stopPolling()
        startPolling()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(VhalConstants.TEMPERATURE_POLL_INTERVAL_MS)
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
            val result = withContext(Dispatchers.IO) { repository.readTemperature() }
            val widgetRank = repository.getStatusIconRank()
            val enabled = repository.isEnabled()

            _uiState.update {
                it.copy(
                    isEnabled = enabled,
                    widgetRank = widgetRank,
                    canStepWidgetLeft = TemperatureWidgetRank.canStepLeft(widgetRank),
                    canStepWidgetRight = TemperatureWidgetRank.canStepRight(widgetRank),
                    statusText = buildStatusText(result),
                    latestTemperatureText = buildLatestTemperatureText(result),
                    temperatureC = result.takeIf { it.ok }?.value?.roundToInt(),
                )
            }
        }
    }

    private fun buildStatusText(result: TemperatureReader.Result): String {
        return if (result.ok) {
            appContext.getString(R.string.temperature_status_ok, result.source)
        } else {
            appContext.getString(R.string.temperature_status_error, result.source)
        }
    }

    private fun buildLatestTemperatureText(result: TemperatureReader.Result): String {
        return if (result.ok) {
            appContext.getString(R.string.temperature_latest_value, result.value.roundToInt())
        } else {
            appContext.getString(R.string.temperature_latest_unavailable)
        }
    }
}
