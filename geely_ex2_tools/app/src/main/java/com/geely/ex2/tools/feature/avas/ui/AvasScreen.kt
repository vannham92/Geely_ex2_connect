package com.geely.ex2.tools.feature.avas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.geely.ex2.tools.feature.avas.AvasViewModel
import com.geely.ex2.tools.ui.components.FlymeSettingsSection
import com.geely.ex2.tools.ui.components.FlymeSettingsSegmentedItem
import com.geely.ex2.tools.ui.components.GeelyTopAppBar
import com.geely.ex2.tools.ui.components.TabVisibilityEffect
import com.geely.ex2.tools.ui.components.isFlymeRailCompact
import com.geely.ex2.tools.ui.rememberOnClickWithSystemSound
import com.geely.ex2.tools.ui.theme.GeelyEx2ToolsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvasScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AvasViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val muteOptions = listOf(
        stringResource(R.string.avas_segment_on),
        stringResource(R.string.avas_segment_off),
    )

    TabVisibilityEffect(
        onVisible = viewModel::onResume,
        onHidden = viewModel::onPause,
    )

    if (uiState.showMuteConfirm) {
        val onMuteConfirmAcceptedWithSound = rememberOnClickWithSystemSound(viewModel::onMuteConfirmAccepted)
        val onMuteConfirmDismissWithSound = rememberOnClickWithSystemSound(viewModel::onMuteConfirmDismiss)

        AlertDialog(
            onDismissRequest = viewModel::onMuteConfirmDismiss,
            title = { Text(stringResource(R.string.avas_mute_confirm_title)) },
            text = { Text(stringResource(R.string.avas_mute_confirm_message)) },
            confirmButton = {
                TextButton(onClick = onMuteConfirmAcceptedWithSound) {
                    Text(stringResource(R.string.avas_mute_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = onMuteConfirmDismissWithSound) {
                    Text(stringResource(R.string.avas_mute_confirm_no))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            GeelyTopAppBar(
                title = stringResource(R.string.avas_screen_title),
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
            FlymeSettingsSection(title = stringResource(R.string.avas_section_control)) {
                FlymeSettingsSegmentedItem(
                    title = stringResource(R.string.avas_mute_title),
                    summary = if (uiState.isMuted) {
                        stringResource(R.string.avas_mute_summary_on)
                    } else {
                        stringResource(R.string.avas_mute_summary_off)
                    },
                    options = muteOptions,
                    selectedIndex = if (uiState.isMuted) 1 else 0,
                    onSelectedIndexChange = viewModel::onMuteSegmentSelected,
                    enabled = !uiState.isChanging,
                    showDivider = false,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "AVAS Light")
@Composable
private fun AvasScreenPreviewLight() {
    GeelyEx2ToolsTheme(darkTheme = false) {
        AvasScreen(onBack = {})
    }
}

@Preview(showBackground = true, name = "AVAS Dark")
@Composable
private fun AvasScreenPreviewDark() {
    GeelyEx2ToolsTheme(darkTheme = true) {
        AvasScreen(onBack = {})
    }
}
