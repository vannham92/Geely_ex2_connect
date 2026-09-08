package com.geely.ex2.tools.feature.speed.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import com.geely.ex2.tools.data.speed.SpeedWidgetRank
import com.geely.ex2.tools.feature.speed.SpeedViewModel
import com.geely.ex2.tools.ui.components.FlymeSettingsSection
import com.geely.ex2.tools.ui.components.FlymeSettingsStepperItem
import com.geely.ex2.tools.ui.components.FlymeSettingsSwitchItem
import com.geely.ex2.tools.ui.components.GeelyTopAppBar
import com.geely.ex2.tools.ui.components.TabVisibilityEffect
import com.geely.ex2.tools.ui.components.isFlymeRailCompact
import com.geely.ex2.tools.ui.theme.GeelyEx2ToolsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpeedViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TabVisibilityEffect(
        onVisible = viewModel::onResume,
        onHidden = viewModel::onPause,
    )

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            GeelyTopAppBar(
                title = stringResource(R.string.speed_screen_title),
                onBack = onBack.takeIf { isFlymeRailCompact() },
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
            FlymeSettingsSection(title = stringResource(R.string.speed_section_control)) {
                FlymeSettingsSwitchItem(
                    title = stringResource(R.string.speed_enable_title),
                    summary = if (uiState.isEnabled) {
                        stringResource(R.string.speed_enable_summary_on)
                    } else {
                        stringResource(R.string.speed_enable_summary_off)
                    },
                    checked = uiState.isEnabled,
                    onCheckedChange = viewModel::onEnabledCheckedChange,
                    showDivider = false,
                )
            }

            FlymeSettingsSection(title = stringResource(R.string.speed_section_icon)) {
                FlymeSettingsStepperItem(
                    title = stringResource(R.string.speed_widget_position_title),
                    summary = stringResource(
                        R.string.speed_widget_position_summary_rank,
                        uiState.widgetRank,
                    ),
                    onStepLeft = { viewModel.onWidgetRankStep(-SpeedWidgetRank.STEP) },
                    onStepRight = { viewModel.onWidgetRankStep(SpeedWidgetRank.STEP) },
                    canStepLeft = uiState.canStepWidgetLeft,
                    canStepRight = uiState.canStepWidgetRight,
                    leftContentDescription = stringResource(R.string.speed_widget_position_step_left),
                    rightContentDescription = stringResource(R.string.speed_widget_position_step_right),
                    enabled = uiState.isEnabled,
                    showDivider = false,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Speed Light")
@Composable
private fun SpeedScreenPreviewLight() {
    GeelyEx2ToolsTheme(darkTheme = false) {
        SpeedScreen(onBack = {})
    }
}

@Preview(showBackground = true, name = "Speed Dark")
@Composable
private fun SpeedScreenPreviewDark() {
    GeelyEx2ToolsTheme(darkTheme = true) {
        SpeedScreen(onBack = {})
    }
}
