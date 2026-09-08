package com.geely.ex2.tools.feature.battery.ui

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
import com.geely.ex2.tools.data.battery.BatteryWidgetRank
import com.geely.ex2.tools.feature.battery.BatteryViewModel
import com.geely.ex2.tools.ui.components.FlymeSettingsSection
import com.geely.ex2.tools.ui.components.FlymeSettingsStepperItem
import com.geely.ex2.tools.ui.components.FlymeSettingsSwitchItem
import com.geely.ex2.tools.ui.components.GeelyTopAppBar
import com.geely.ex2.tools.ui.components.TabVisibilityEffect
import com.geely.ex2.tools.ui.components.isFlymeRailCompact
import com.geely.ex2.tools.ui.theme.GeelyEx2ToolsTheme
import com.geely.ex2.tools.ui.theme.StatusGreen
import com.geely.ex2.tools.ui.theme.StatusOrange
import com.geely.ex2.tools.ui.theme.StatusRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BatteryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TabVisibilityEffect(
        onVisible = viewModel::onResume,
        onHidden = viewModel::onPause,
    )

    val socPercent = uiState.socPercent
    val socColor = when {
        socPercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
        socPercent >= 50 -> StatusGreen
        socPercent >= 21 -> StatusOrange
        else -> StatusRed
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            GeelyTopAppBar(
                title = stringResource(R.string.battery_screen_title),
                onBack = onBack.takeIf { isFlymeRailCompact() },
                titleTrailing = socPercent?.let {
                    {
                        Text(
                            text = uiState.latestSocText,
                            style = MaterialTheme.typography.titleMedium,
                            color = socColor,
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
            FlymeSettingsSection(title = stringResource(R.string.battery_section_control)) {
                FlymeSettingsSwitchItem(
                    title = stringResource(R.string.battery_enable_title),
                    summary = if (uiState.isEnabled) {
                        stringResource(R.string.battery_enable_summary_on)
                    } else {
                        stringResource(R.string.battery_enable_summary_off)
                    },
                    checked = uiState.isEnabled,
                    onCheckedChange = viewModel::onEnabledCheckedChange,
                    showDivider = false,
                )
            }

            FlymeSettingsSection(title = stringResource(R.string.battery_section_icon)) {
                FlymeSettingsStepperItem(
                    title = stringResource(R.string.battery_widget_position_title),
                    summary = stringResource(
                        R.string.battery_widget_position_summary_rank,
                        uiState.widgetRank,
                    ),
                    onStepLeft = { viewModel.onWidgetRankStep(-BatteryWidgetRank.STEP) },
                    onStepRight = { viewModel.onWidgetRankStep(BatteryWidgetRank.STEP) },
                    canStepLeft = uiState.canStepWidgetLeft,
                    canStepRight = uiState.canStepWidgetRight,
                    leftContentDescription = stringResource(R.string.battery_widget_position_step_left),
                    rightContentDescription = stringResource(R.string.battery_widget_position_step_right),
                    enabled = uiState.isEnabled,
                    showDivider = false,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Battery Light")
@Composable
private fun BatteryScreenPreviewLight() {
    GeelyEx2ToolsTheme(darkTheme = false) {
        BatteryScreen(onBack = {})
    }
}

@Preview(showBackground = true, name = "Battery Dark")
@Composable
private fun BatteryScreenPreviewDark() {
    GeelyEx2ToolsTheme(darkTheme = true) {
        BatteryScreen(onBack = {})
    }
}
