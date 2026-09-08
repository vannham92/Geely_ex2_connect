package com.geely.ex2.tools.feature.wifi

import android.app.Application
import android.net.wifi.WifiManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geely.ex2.tools.data.vhal.VhalConstants
import com.geely.ex2.tools.data.wifi.WifiRepository
import com.geely.ex2.tools.data.wifi.WifiWidgetRank
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class WifiUiState(
    val wifiState: Int = WifiManager.WIFI_STATE_UNKNOWN,
    val wifiStateLabel: String = "",
    val isAutoEnableEnabled: Boolean = true,
    val isWifiOn: Boolean = false,
    val ipAddress: String? = null,
    val widgetRank: Int = WifiWidgetRank.DEFAULT,
    val canStepWidgetLeft: Boolean = true,
    val canStepWidgetRight: Boolean = true,
)

class WifiViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WifiRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(WifiUiState())
    val uiState: StateFlow<WifiUiState> = _uiState.asStateFlow()

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

    fun onWifiCheckedChange(enabled: Boolean) {
        if (enabled == _uiState.value.isWifiOn) return
        repository.setWifiEnabledFromUi(enabled)
        if (repository.isAutoEnableEnabled()) {
            repository.notifyStatusIcon("UI Wi-Fi switch")
        }
        refreshState()
    }

    fun onAutoEnableCheckedChange(enabled: Boolean) {
        if (enabled == _uiState.value.isAutoEnableEnabled) return
        repository.setAutoEnableEnabled(enabled)
        if (enabled) {
            syncBackgroundWork("UI auto-enable switch")
            restartPolling()
        } else {
            stopBackgroundWork("UI auto-enable switch")
            stopPolling()
        }
        refreshState()
    }

    fun onWidgetRankStep(delta: Int) {
        val currentRank = _uiState.value.widgetRank
        val canStep = if (delta < 0) {
            WifiWidgetRank.canStepLeft(currentRank)
        } else {
            WifiWidgetRank.canStepRight(currentRank)
        }
        if (!canStep) return

        val newRank = repository.stepStatusIconRank(delta)
        if (repository.isAutoEnableEnabled()) {
            repository.notifyStatusIcon("UI widget rank step", newRank)
        }
        _uiState.update {
            it.copy(
                widgetRank = newRank,
                canStepWidgetLeft = WifiWidgetRank.canStepLeft(newRank),
                canStepWidgetRight = WifiWidgetRank.canStepRight(newRank),
            )
        }
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }

    private fun refreshState() {
        val wifiState = repository.getWifiState()
        val isAutoEnable = repository.isAutoEnableEnabled()
        val isWifiOn = repository.isWifiEnabledOrEnabling()
        val ipAddress = if (isWifiOn) repository.getWifiIpAddress() else null
        val widgetRank = repository.getStatusIconRank()

        _uiState.update {
            WifiUiState(
                wifiState = wifiState,
                wifiStateLabel = repository.wifiStateToLabel(wifiState),
                isAutoEnableEnabled = isAutoEnable,
                isWifiOn = isWifiOn,
                ipAddress = ipAddress,
                widgetRank = widgetRank,
                canStepWidgetLeft = WifiWidgetRank.canStepLeft(widgetRank),
                canStepWidgetRight = WifiWidgetRank.canStepRight(widgetRank),
            )
        }
    }

    private fun syncBackgroundWork(reason: String) {
        if (!repository.isAutoEnableEnabled()) {
            stopBackgroundWork("$reason, auto-enable disabled")
            return
        }
        repository.startStatusService(reason)
        repository.notifyStatusIcon(reason)
    }

    private fun stopBackgroundWork(reason: String) {
        repository.stopStatusService(reason)
        repository.cancelStatusIcon()
    }

    private fun restartPolling() {
        stopPolling()
        if (repository.isAutoEnableEnabled()) {
            startPolling()
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(VhalConstants.WIFI_UI_POLL_INTERVAL_MS)
                refreshState()
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }
}
