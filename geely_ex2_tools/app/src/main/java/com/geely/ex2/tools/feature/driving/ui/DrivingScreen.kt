package com.geely.ex2.tools.feature.driving.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import com.geely.ex2.tools.ui.components.GeelyTopAppBar
import com.geely.ex2.tools.ui.components.isFlymeRailCompact
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
import com.geely.ex2.tools.data.vhal.DrivingMode
import com.geely.ex2.tools.data.vhal.EnergyRegeneration
import com.geely.ex2.tools.feature.driving.DrivingViewModel
import com.geely.ex2.tools.ui.components.FlymeSettingsSection
import com.geely.ex2.tools.ui.components.FlymeSettingsSegmentedItem
import com.geely.ex2.tools.ui.components.FlymeSettingsSwitchItem
import com.geely.ex2.tools.ui.components.TabVisibilityEffect
import com.geely.ex2.tools.ui.theme.GeelyEx2ToolsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrivingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DrivingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val modeLabels = DrivingMode.selectable.map { stringResource(it.labelRes) }
    val regenLabels = EnergyRegeneration.selectable.map { stringResource(it.labelRes) }

    TabVisibilityEffect(
        onVisible = viewModel::onResume,
        onHidden = viewModel::onPause,
    )

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            GeelyTopAppBar(
                title = stringResource(R.string.driving_screen_title),
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
            FlymeSettingsSection(title = stringResource(R.string.driving_section_mode)) {
                FlymeSettingsSegmentedItem(
                    title = stringResource(R.string.driving_mode_switch_title),
                    summary = stringResource(R.string.driving_mode_switch_summary),
                    options = modeLabels,
                    selectedIndex = uiState.selectedIndex,
                    onSelectedIndexChange = viewModel::onModeSelected,
                    enabled = uiState.isWritable && !uiState.isChangingMode,
                    showDivider = true,
                )
                FlymeSettingsSwitchItem(
                    title = stringResource(R.string.driving_persist_title),
                    checked = uiState.isPersistEnabled,
                    onCheckedChange = viewModel::onPersistCheckedChange,
                    showDivider = false,
                )
            }

            FlymeSettingsSection(title = stringResource(R.string.driving_section_regen)) {
                FlymeSettingsSegmentedItem(
                    title = stringResource(R.string.driving_regen_switch_title),
                    summary = stringResource(R.string.driving_regen_switch_summary),
                    options = regenLabels,
                    selectedIndex = uiState.regenSelectedIndex,
                    onSelectedIndexChange = viewModel::onRegenSelected,
                    enabled = uiState.isRegenWritable && !uiState.isChangingRegen,
                    showDivider = true,
                )
                FlymeSettingsSwitchItem(
                    title = stringResource(R.string.driving_regen_persist_title),
                    checked = uiState.isRegenPersistEnabled,
                    onCheckedChange = viewModel::onRegenPersistCheckedChange,
                    showDivider = false,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Driving Light")
@Composable
private fun DrivingScreenPreviewLight() {
    GeelyEx2ToolsTheme(darkTheme = false) {
        DrivingScreen(onBack = {})
    }
}

@Preview(showBackground = true, name = "Driving Dark")
@Composable
private fun DrivingScreenPreviewDark() {
    GeelyEx2ToolsTheme(darkTheme = true) {
        DrivingScreen(onBack = {})
    }
}
