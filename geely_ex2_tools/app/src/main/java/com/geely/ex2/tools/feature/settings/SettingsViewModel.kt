package com.geely.ex2.tools.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.geely.ex2.tools.data.settings.AppLocale
import com.geely.ex2.tools.data.settings.AppLocaleController
import com.geely.ex2.tools.data.settings.AppLocaleSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val selectedLocale: AppLocale = AppLocale.SYSTEM,
    val selectedIndex: Int = 0,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(readState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onLocaleSelected(index: Int) {
        val locale = AppLocale.entries.getOrNull(index) ?: return
        if (locale == _uiState.value.selectedLocale) return
        val appContext = getApplication<Application>()
        AppLocaleSettings.set(appContext, locale)
        _uiState.update {
            it.copy(
                selectedLocale = locale,
                selectedIndex = index,
            )
        }
        AppLocaleController.apply(locale)
    }

    private fun readState(): SettingsUiState {
        val selected = AppLocaleSettings.get(getApplication())
        return SettingsUiState(
            selectedLocale = selected,
            selectedIndex = AppLocale.entries.indexOf(selected).coerceAtLeast(0),
        )
    }
}
