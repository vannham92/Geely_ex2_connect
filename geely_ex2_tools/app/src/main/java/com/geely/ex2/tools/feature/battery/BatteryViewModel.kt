package com.geely.ex2.tools.feature.battery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geely.ex2.tools.R
import com.geely.ex2.tools.data.battery.BatteryRepository
import com.geely.ex2.tools.data.battery.BatteryWidgetRank
import com.geely.ex2.tools.data.vhal.BatterySample
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

data class BatteryUiState(
    val isEnabled: Boolean = false,
    val widgetRank: Int = BatteryWidgetRank.DEFAULT,
    val canStepWidgetLeft: Boolean = true,
    val canStepWidgetRight: Boolean = true,
    val statusText: String = "",
    val latestSocText: String = "",
    val socPercent: Int? = null,
)

class BatteryViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = BatteryRepository(appContext)

    private val _uiState = MutableStateFlow(BatteryUiState())
    val uiState: StateFlow<BatteryUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var resumeJob: Job? = null

    fun onResume() {
        resumeJob?.cancel()
        resumeJob = viewModelScope.launch {
            refreshState()
            if (!isActive) return@launch
            restartPolling()
        }
    }

    fun onPause() {
        resumeJob?.cancel()
        resumeJob = null
        stopPolling()
    }

    fun onEnabledCheckedChange(enabled: Boolean) {
        if (enabled == _uiState.value.isEnabled) return
        repository.setEnabled(enabled)
        // The toggle only controls the status-bar icon/service; the in-app value always shows.
        if (enabled) {
            syncBackgroundWork("UI battery enable")
        } else {
            repository.stopStatusService("UI battery disable")
            repository.cancelStatusIcon()
        }
        refreshState()
    }

    fun onWidgetRankStep(delta: Int) {
        val currentRank = _uiState.value.widgetRank
        val canStep = if (delta < 0) {
            BatteryWidgetRank.canStepLeft(currentRank)
        } else {
            BatteryWidgetRank.canStepRight(currentRank)
        }
        if (!canStep) return

        val newRank = repository.stepStatusIconRank(delta)
        if (_uiState.value.isEnabled) {
            repository.notifyStatusIconIfEnabled("UI widget rank step", newRank)
        }
        _uiState.update {
            it.copy(
                widgetRank = newRank,
                canStepWidgetLeft = BatteryWidgetRank.canStepLeft(newRank),
                canStepWidgetRight = BatteryWidgetRank.canStepRight(newRank),
            )
        }
    }

    override fun onCleared() {
        stopPolling()
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
                delay(VhalConstants.BATTERY_POLL_INTERVAL_MS)
                refreshState()
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun refreshState() {
        viewModelScope.launch {
            val sample = withContext(Dispatchers.IO) { repository.readBatterySocDirect() }
            val enabled = repository.isEnabled()
            val widgetRank = repository.getStatusIconRank()
            _uiState.update {
                it.copy(
                    isEnabled = enabled,
                    widgetRank = widgetRank,
                    canStepWidgetLeft = BatteryWidgetRank.canStepLeft(widgetRank),
                    canStepWidgetRight = BatteryWidgetRank.canStepRight(widgetRank),
                    statusText = buildStatusText(sample),
                    latestSocText = buildLatestSocText(sample),
                    socPercent = sample.takeIf { it.isAvailable }?.socPercent?.toInt(),
                )
            }
        }
    }

    private fun buildStatusText(sample: BatterySample): String {
        return if (sample.isAvailable) {
            appContext.getString(R.string.battery_status_ok, sample.source)
        } else {
            appContext.getString(R.string.battery_status_error, sample.source)
        }
    }

    private fun buildLatestSocText(sample: BatterySample): String {
        return if (sample.isAvailable) {
            appContext.getString(R.string.battery_latest_value, sample.socPercent.toInt())
        } else {
            appContext.getString(R.string.battery_latest_unavailable)
        }
    }
}
