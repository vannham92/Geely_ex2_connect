package com.geely.ex2.tools.feature.ambient

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geely.ex2.tools.R
import com.geely.ex2.tools.data.ambient.AmbientLightAppStarter
import com.geely.ex2.tools.data.ambient.AmbientLightControlMode
import com.geely.ex2.tools.data.ambient.AmbientLightRepository
import com.geely.ex2.tools.data.ambient.AmbientLightSchedule
import com.geely.ex2.tools.data.ambient.AmbientLightScheduleController
import com.geely.ex2.tools.data.ambient.AmbientLightSettings
import com.geely.ex2.tools.data.vhal.AmbientLightSample
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

enum class AmbientLightTimePickerTarget {
    START,
    END,
}

data class AmbientLightUiState(
    val isEnabled: Boolean = false,
    val isAvailable: Boolean = false,
    val isWritable: Boolean = true,
    val statusText: String = "",
    val isChanging: Boolean = false,
    val controlMode: AmbientLightControlMode = AmbientLightControlMode.AUTO,
    val controlModeIndex: Int = 0,
    val startHour: Int = 7,
    val startMinute: Int = 0,
    val endHour: Int = 22,
    val endMinute: Int = 0,
    val startTimeText: String = "22:00",
    val endTimeText: String = "07:00",
    val scheduleActiveNow: Boolean = false,
    val scheduleSummary: String = "",
    val timePickerTarget: AmbientLightTimePickerTarget? = null,
)

class AmbientLightViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = AmbientLightRepository(appContext)

    private val _uiState = MutableStateFlow(AmbientLightUiState())
    val uiState: StateFlow<AmbientLightUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var refreshJob: Job? = null
    private var resumeJob: Job? = null

    fun onResume() {
        resumeJob?.cancel()
        resumeJob = viewModelScope.launch {
            syncControlModeFromVehicleIfNeeded()
            refreshState()
            startPolling()
        }
    }

    fun onPause() {
        resumeJob?.cancel()
        resumeJob = null
        stopPolling()
    }

    fun onControlModeSelected(index: Int) {
        val mode = AmbientLightControlMode.fromIndex(index) ?: return
        if (mode == _uiState.value.controlMode) return

        AmbientLightSettings.setControlMode(appContext, mode)
        refreshState()
        AmbientLightScheduleController.syncBackgroundWork(appContext, "UI control mode $mode")
        AmbientLightAppStarter.startRestoreServiceIfEnabled(appContext, "UI control mode $mode")
    }

    fun onStartTimeClick() {
        _uiState.update { it.copy(timePickerTarget = AmbientLightTimePickerTarget.START) }
    }

    fun onEndTimeClick() {
        _uiState.update { it.copy(timePickerTarget = AmbientLightTimePickerTarget.END) }
    }

    fun onTimePickerDismiss() {
        _uiState.update { it.copy(timePickerTarget = null) }
    }

    fun onTimePickerConfirm(hour: Int, minute: Int) {
        val target = _uiState.value.timePickerTarget ?: return
        when (target) {
            AmbientLightTimePickerTarget.START -> AmbientLightSettings.setStartTime(appContext, hour, minute)
            AmbientLightTimePickerTarget.END -> AmbientLightSettings.setEndTime(appContext, hour, minute)
        }
        _uiState.update { it.copy(timePickerTarget = null) }
        refreshState()
        if (AmbientLightSettings.isScheduleEnabled(appContext)) {
            AmbientLightScheduleController.applyIfNeeded(appContext, "UI time changed")
        }
    }

    override fun onCleared() {
        stopPolling()
        refreshJob?.cancel()
        super.onCleared()
    }

    private suspend fun syncControlModeFromVehicleIfNeeded() {
        if (AmbientLightSettings.isAutoModeSaved(appContext)) {
            return
        }

        val sample = withContext(Dispatchers.IO) {
            repository.readAmbientLight()
        }
        if (!sample.isAvailable) {
            return
        }

        val mode = if (sample.isEnabled) {
            AmbientLightControlMode.ON
        } else {
            AmbientLightControlMode.OFF
        }
        AmbientLightSettings.syncSessionModeFromVehicle(mode)
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(VhalConstants.AMBIENT_LIGHT_UI_POLL_INTERVAL_MS)
                refreshState()
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun refreshState(writeError: String? = null) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val sample = withContext(Dispatchers.IO) {
                repository.readAmbientLight()
            }
            _uiState.update {
                buildUiState(sample, writeError)
            }
        }
    }

    private fun buildUiState(sample: AmbientLightSample, writeError: String?): AmbientLightUiState {
        val controlMode = AmbientLightSettings.getControlMode(appContext)
        val startHour = AmbientLightSettings.getStartHour(appContext)
        val startMinute = AmbientLightSettings.getStartMinute(appContext)
        val endHour = AmbientLightSettings.getEndHour(appContext)
        val endMinute = AmbientLightSettings.getEndMinute(appContext)
        val scheduleActiveNow = AmbientLightSchedule.shouldBeEnabled(
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
            minute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE),
        )

        return AmbientLightUiState(
            isEnabled = sample.isEnabled,
            isAvailable = sample.isAvailable,
            isWritable = sample.isAvailable && !_uiState.value.isChanging,
            statusText = buildStatusText(sample, writeError, controlMode, scheduleActiveNow),
            isChanging = _uiState.value.isChanging,
            controlMode = controlMode,
            controlModeIndex = AmbientLightControlMode.indexOf(controlMode),
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            startTimeText = AmbientLightSchedule.formatTime(startHour, startMinute),
            endTimeText = AmbientLightSchedule.formatTime(endHour, endMinute),
            scheduleActiveNow = scheduleActiveNow,
            scheduleSummary = buildScheduleSummary(controlMode, scheduleActiveNow),
            timePickerTarget = _uiState.value.timePickerTarget,
        )
    }

    private fun buildScheduleSummary(
        controlMode: AmbientLightControlMode,
        scheduleActiveNow: Boolean,
    ): String {
        return when (controlMode) {
            AmbientLightControlMode.AUTO -> {
                if (scheduleActiveNow) {
                    appContext.getString(R.string.ambient_light_schedule_active_on)
                } else {
                    appContext.getString(R.string.ambient_light_schedule_active_off)
                }
            }
            AmbientLightControlMode.OFF ->
                appContext.getString(R.string.ambient_light_mode_summary_off)
            AmbientLightControlMode.ON ->
                appContext.getString(R.string.ambient_light_mode_summary_on)
        }
    }

    private fun buildStatusText(
        sample: AmbientLightSample,
        writeError: String?,
        controlMode: AmbientLightControlMode,
        scheduleActiveNow: Boolean,
    ): String {
        if (writeError != null) {
            return appContext.getString(R.string.ambient_light_status_write_error, writeError)
        }
        if (!sample.isAvailable) {
            return appContext.getString(R.string.ambient_light_status_error, sample.source)
        }
        if (controlMode == AmbientLightControlMode.AUTO) {
            return appContext.getString(
                R.string.ambient_light_status_ok_schedule,
                sample.source,
                if (scheduleActiveNow) {
                    appContext.getString(R.string.ambient_light_header_on)
                } else {
                    appContext.getString(R.string.ambient_light_header_off)
                },
            )
        }
        return appContext.getString(R.string.ambient_light_status_ok, sample.source)
    }
}
