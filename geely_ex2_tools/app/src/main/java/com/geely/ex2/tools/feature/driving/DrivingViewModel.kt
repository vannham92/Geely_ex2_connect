package com.geely.ex2.tools.feature.driving

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.geely.ex2.tools.R
import com.geely.ex2.tools.data.driving.DrivingModeRepository
import com.geely.ex2.tools.data.vhal.DrivingMode
import com.geely.ex2.tools.data.vhal.DrivingModeSample
import com.geely.ex2.tools.data.vhal.EnergyRegeneration
import com.geely.ex2.tools.data.vhal.EnergyRegenerationSample
import com.geely.ex2.tools.data.vhal.VhalConstants
import kotlinx.coroutines.CancellationException
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
import java.util.Locale

data class DrivingUiState(
    val currentModeText: String = "",
    val selectedIndex: Int = -1,
    val isAvailable: Boolean = false,
    val isWritable: Boolean = true,
    val isPersistEnabled: Boolean = true,
    val savedModeText: String = "",
    val statusText: String = "",
    val rawValueText: String = "",
    val isChangingMode: Boolean = false,
    val currentRegenText: String = "",
    val regenSelectedIndex: Int = -1,
    val isRegenAvailable: Boolean = false,
    val isRegenWritable: Boolean = true,
    val isRegenPersistEnabled: Boolean = true,
    val savedRegenText: String = "",
    val regenStatusText: String = "",
    val regenRawValueText: String = "",
    val isChangingRegen: Boolean = false,
)

class DrivingViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = DrivingModeRepository(appContext)

    private val _uiState = MutableStateFlow(DrivingUiState())
    val uiState: StateFlow<DrivingUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var refreshJob: Job? = null

    fun onResume() {
        refreshState()
        startPolling()
    }

    fun onPause() {
        stopPolling()
    }

    fun onPersistCheckedChange(enabled: Boolean) {
        if (enabled == _uiState.value.isPersistEnabled) return
        repository.setPersistEnabled(enabled)
        viewModelScope.launch {
            if (enabled) {
                val sample = withContext(Dispatchers.IO) {
                    repository.readDrivingMode()
                }
                val modeToSave = when {
                    sample.isAvailable && DrivingMode.isSelectableValue(sample.modeValue) ->
                        sample.modeValue
                    DrivingMode.isSelectableValue(repository.getSavedModeValue()) ->
                        repository.getSavedModeValue()
                    else ->
                        VhalConstants.DRIVE_MODE_COMFORT
                }
                repository.saveSelectedMode(modeToSave)
                repository.restoreSavedModeIfNeeded("UI persist enable")
            } else {
                repository.stopRestoreService("UI mode persist disable")
            }
            startRestoreService("UI mode persist toggle")
            refreshState()
        }
    }

    fun onRegenPersistCheckedChange(enabled: Boolean) {
        if (enabled == _uiState.value.isRegenPersistEnabled) return
        repository.setRegenPersistEnabled(enabled)
        viewModelScope.launch {
            if (enabled) {
                val sample = withContext(Dispatchers.IO) {
                    repository.readEnergyRegeneration()
                }
                val levelToSave = when {
                    sample.isAvailable && EnergyRegeneration.isSelectableValue(sample.levelValue) ->
                        sample.levelValue
                    EnergyRegeneration.isSelectableValue(repository.getSavedRegenValue()) ->
                        repository.getSavedRegenValue()
                    else ->
                        VhalConstants.ENERGY_REGENERATION_LEVEL_MID
                }
                repository.saveSelectedRegen(levelToSave)
                repository.restoreSavedRegenIfNeeded("UI regen persist enable")
            } else {
                repository.stopRestoreService("UI regen persist disable")
            }
            startRestoreService("UI regen persist toggle")
            refreshState()
        }
    }

    fun onModeSelected(index: Int) {
        if (index !in DrivingMode.selectable.indices) return
        if (_uiState.value.isChangingMode) return

        val option = DrivingMode.selectable[index]
        if (index == _uiState.value.selectedIndex) return

        _uiState.update { it.copy(isChangingMode = true) }
        viewModelScope.launch {
            var writeError: String? = null
            var wroteOk = false
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.setDrivingMode(option.vhalValue)
                }
                if (result.ok) {
                    repository.saveSelectedMode(option.vhalValue)
                    wroteOk = true
                } else {
                    writeError = result.error
                        ?: appContext.getString(R.string.driving_write_error_unknown)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                writeError = t.message
                    ?: appContext.getString(R.string.driving_write_error_unknown)
            } finally {
                _uiState.update {
                    it.copy(
                        isChangingMode = false,
                        selectedIndex = if (wroteOk) index else it.selectedIndex,
                        currentModeText = if (wroteOk) {
                            appContext.getString(option.labelRes)
                        } else {
                            it.currentModeText
                        },
                    )
                }
            }
            // Success: keep optimistic UI. Immediate read is often still stale on eCarX.
            if (writeError != null) {
                refreshState(writeError = writeError)
            }
        }
    }

    fun onRegenSelected(index: Int) {
        if (index !in EnergyRegeneration.selectable.indices) return
        if (_uiState.value.isChangingRegen) return

        val option = EnergyRegeneration.selectable[index]
        if (index == _uiState.value.regenSelectedIndex) return

        _uiState.update { it.copy(isChangingRegen = true) }
        viewModelScope.launch {
            var regenWriteError: String? = null
            var wroteOk = false
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.setEnergyRegeneration(option.vhalValue)
                }
                if (result.ok) {
                    repository.saveSelectedRegen(option.vhalValue)
                    wroteOk = true
                } else {
                    regenWriteError = result.error
                        ?: appContext.getString(R.string.driving_write_error_unknown)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                regenWriteError = t.message
                    ?: appContext.getString(R.string.driving_write_error_unknown)
            } finally {
                _uiState.update {
                    it.copy(
                        isChangingRegen = false,
                        regenSelectedIndex = if (wroteOk) index else it.regenSelectedIndex,
                        currentRegenText = if (wroteOk) {
                            appContext.getString(option.labelRes)
                        } else {
                            it.currentRegenText
                        },
                    )
                }
            }
            if (regenWriteError != null) {
                refreshState(regenWriteError = regenWriteError)
            }
        }
    }

    override fun onCleared() {
        stopPolling()
        refreshJob?.cancel()
        super.onCleared()
    }

    private fun startRestoreService(reason: String) {
        if (repository.isPersistEnabled() || repository.isRegenPersistEnabled()) {
            repository.startRestoreService(reason)
        } else {
            repository.stopRestoreService(reason)
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(VhalConstants.DRIVING_UI_POLL_INTERVAL_MS)
                refreshState()
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun refreshState(
        writeError: String? = null,
        regenWriteError: String? = null,
    ) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val (modeSample, regenSample) = withContext(Dispatchers.IO) {
                repository.readDrivingMode() to repository.readEnergyRegeneration()
            }
            _uiState.update {
                buildUiState(
                    sample = modeSample,
                    regenSample = regenSample,
                    writeError = writeError,
                    regenWriteError = regenWriteError,
                    isChangingMode = it.isChangingMode,
                    isChangingRegen = it.isChangingRegen,
                )
            }
        }
    }

    private fun buildUiState(
        sample: DrivingModeSample,
        regenSample: EnergyRegenerationSample,
        writeError: String?,
        regenWriteError: String?,
        isChangingMode: Boolean,
        isChangingRegen: Boolean,
    ): DrivingUiState {
        val persistEnabled = repository.isPersistEnabled()
        val savedModeValue = repository.getSavedModeValue()
        val selectedIndex = if (sample.isAvailable) {
            DrivingMode.selectableIndexFor(sample.modeValue)
        } else {
            -1
        }

        val regenPersistEnabled = repository.isRegenPersistEnabled()
        val savedRegenValue = repository.getSavedRegenValue()
        val regenSelectedIndex = if (regenSample.isAvailable) {
            EnergyRegeneration.selectableIndexFor(regenSample.levelValue)
        } else {
            -1
        }

        return DrivingUiState(
            currentModeText = buildCurrentModeText(sample),
            selectedIndex = selectedIndex,
            isAvailable = sample.isAvailable,
            isWritable = sample.isAvailable && !isChangingMode,
            isPersistEnabled = persistEnabled,
            savedModeText = buildSavedModeText(savedModeValue),
            statusText = buildStatusText(sample, writeError, persistEnabled),
            rawValueText = buildRawValueText(sample),
            isChangingMode = isChangingMode,
            currentRegenText = buildCurrentRegenText(regenSample),
            regenSelectedIndex = regenSelectedIndex,
            isRegenAvailable = regenSample.isAvailable,
            isRegenWritable = regenSample.isAvailable && !isChangingRegen,
            isRegenPersistEnabled = regenPersistEnabled,
            savedRegenText = buildSavedRegenText(savedRegenValue),
            regenStatusText = buildRegenStatusText(regenSample, regenWriteError, regenPersistEnabled),
            regenRawValueText = buildRegenRawValueText(regenSample),
            isChangingRegen = isChangingRegen,
        )
    }

    private fun buildSavedModeText(savedModeValue: Int): String {
        val labelRes = DrivingMode.labelResFor(savedModeValue)
        return if (labelRes != null) {
            appContext.getString(labelRes)
        } else {
            appContext.getString(
                R.string.driving_current_unknown,
                String.format(Locale.US, "0x%08X", savedModeValue),
            )
        }
    }

    private fun buildSavedRegenText(savedRegenValue: Int): String {
        val labelRes = EnergyRegeneration.labelResFor(savedRegenValue)
        return if (labelRes != null) {
            appContext.getString(labelRes)
        } else {
            appContext.getString(
                R.string.driving_regen_current_unknown,
                String.format(Locale.US, "0x%08X", savedRegenValue),
            )
        }
    }

    private fun buildCurrentModeText(sample: DrivingModeSample): String {
        if (!sample.isAvailable) {
            return appContext.getString(R.string.driving_current_unavailable)
        }

        val labelRes = DrivingMode.labelResFor(sample.modeValue)
        return if (labelRes != null) {
            appContext.getString(labelRes)
        } else {
            appContext.getString(
                R.string.driving_current_unknown,
                String.format(Locale.US, "0x%08X", sample.modeValue),
            )
        }
    }

    private fun buildCurrentRegenText(sample: EnergyRegenerationSample): String {
        if (!sample.isAvailable) {
            return appContext.getString(R.string.driving_regen_current_unavailable)
        }

        val labelRes = EnergyRegeneration.labelResFor(sample.levelValue)
        return if (labelRes != null) {
            appContext.getString(labelRes)
        } else {
            appContext.getString(
                R.string.driving_regen_current_unknown,
                String.format(Locale.US, "0x%08X", sample.levelValue),
            )
        }
    }

    private fun buildStatusText(
        sample: DrivingModeSample,
        writeError: String?,
        persistEnabled: Boolean,
    ): String {
        if (writeError != null) {
            return appContext.getString(R.string.driving_status_write_error, writeError)
        }
        if (!sample.isAvailable) {
            return appContext.getString(R.string.driving_status_error, sample.source)
        }
        if (persistEnabled) {
            return appContext.getString(R.string.driving_status_ok_persist, sample.source)
        }
        return appContext.getString(R.string.driving_status_ok, sample.source)
    }

    private fun buildRegenStatusText(
        sample: EnergyRegenerationSample,
        writeError: String?,
        persistEnabled: Boolean,
    ): String {
        if (writeError != null) {
            return appContext.getString(R.string.driving_status_write_error, writeError)
        }
        if (!sample.isAvailable) {
            return appContext.getString(R.string.driving_status_error, sample.source)
        }
        if (persistEnabled) {
            return appContext.getString(R.string.driving_regen_status_ok_persist, sample.source)
        }
        return appContext.getString(R.string.driving_status_ok, sample.source)
    }

    private fun buildRawValueText(sample: DrivingModeSample): String {
        if (!sample.isAvailable) {
            return appContext.getString(R.string.driving_raw_unavailable)
        }
        return String.format(Locale.US, "0x%08X (%d)", sample.modeValue, sample.modeValue)
    }

    private fun buildRegenRawValueText(sample: EnergyRegenerationSample): String {
        if (!sample.isAvailable) {
            return appContext.getString(R.string.driving_raw_unavailable)
        }
        return String.format(Locale.US, "0x%08X (%d)", sample.levelValue, sample.levelValue)
    }
}
