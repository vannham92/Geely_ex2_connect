package com.geely.ex2.tools.feature.temperature.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geely.ex2.tools.R
import com.geely.ex2.tools.data.temperature.TemperatureWidgetRank
import com.geely.ex2.tools.feature.temperature.TemperatureViewModel
import com.geely.ex2.tools.ui.components.FlymeSettingsStepperItem
import com.geely.ex2.tools.ui.components.FlymeSettingsSection
import com.geely.ex2.tools.ui.components.FlymeSettingsSwitchItem
import com.geely.ex2.tools.ui.components.GeelyTopAppBar
import com.geely.ex2.tools.ui.components.TabVisibilityEffect
import com.geely.ex2.tools.ui.components.isFlymeRailCompact
import com.geely.ex2.tools.ui.theme.GeelyEx2ToolsTheme
import com.geely.ex2.tools.ui.theme.StatusBlue
import com.geely.ex2.tools.ui.theme.StatusOrange
import com.geely.ex2.tools.ui.theme.StatusRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperatureScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TemperatureViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TabVisibilityEffect(
        onVisible = viewModel::onResume,
        onHidden = viewModel::onPause,
    )

    val temperatureC = uiState.temperatureC
    val temperatureColor = when {
        temperatureC == null -> MaterialTheme.colorScheme.onSurfaceVariant
        temperatureC >= 32 -> StatusRed
        temperatureC >= 29 -> StatusOrange
        else -> StatusBlue
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            GeelyTopAppBar(
                title = stringResource(R.string.temperature_screen_title),
                onBack = onBack.takeIf { isFlymeRailCompact() },
                titleTrailing = temperatureC?.let {
                    {
                        Text(
                            text = uiState.latestTemperatureText,
                            style = MaterialTheme.typography.titleMedium,
                            color = temperatureColor,
                            maxLines = 1,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            FlymeSettingsSection(title = stringResource(R.string.temperature_section_control)) {
                FlymeSettingsSwitchItem(
                    title = stringResource(R.string.temperature_enable_title),
                    summary = if (uiState.isEnabled) {
                        stringResource(R.string.temperature_enable_summary_on)
                    } else {
                        stringResource(R.string.temperature_enable_summary_off)
                    },
                    checked = uiState.isEnabled,
                    onCheckedChange = viewModel::onEnabledCheckedChange,
                    showDivider = false,
                )
            }

            FlymeSettingsSection(title = stringResource(R.string.temperature_section_icon)) {
                FlymeSettingsStepperItem(
                    title = stringResource(R.string.temperature_widget_position_title),
                    summary = stringResource(
                        R.string.temperature_widget_position_summary_rank,
                        uiState.widgetRank,
                    ),
                    onStepLeft = { viewModel.onWidgetRankStep(-TemperatureWidgetRank.STEP) },
                    onStepRight = { viewModel.onWidgetRankStep(TemperatureWidgetRank.STEP) },
                    canStepLeft = uiState.canStepWidgetLeft,
                    canStepRight = uiState.canStepWidgetRight,
                    leftContentDescription = stringResource(R.string.temperature_widget_position_step_left),
                    rightContentDescription = stringResource(R.string.temperature_widget_position_step_right),
                    enabled = uiState.isEnabled,
                    showDivider = false,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Temperature Light")
@Composable
private fun TemperatureScreenPreviewLight() {
    GeelyEx2ToolsTheme(darkTheme = false) {
        TemperatureScreen(onBack = {})
    }
}

@Preview(showBackground = true, name = "Temperature Dark")
@Composable
private fun TemperatureScreenPreviewDark() {
    GeelyEx2ToolsTheme(darkTheme = true) {
        TemperatureScreen(onBack = {})
    }
}
