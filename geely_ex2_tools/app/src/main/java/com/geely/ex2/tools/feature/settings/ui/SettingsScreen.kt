package com.geely.ex2.tools.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geely.ex2.tools.R
import com.geely.ex2.tools.data.app.getAppVersionName
import com.geely.ex2.tools.data.network.CarTcpServer
import com.geely.ex2.tools.data.settings.AppLocale
import com.geely.ex2.tools.data.vhal.CarVhalBindings
import com.geely.ex2.tools.data.vhal.VhalConstants
import com.geely.ex2.tools.data.vhal.VhalSpeedReaderFactory
import com.geely.ex2.tools.data.vhal.VhalBatteryReaderFactory
import com.geely.ex2.tools.feature.home.ui.DeveloperAboutDialog
import com.geely.ex2.tools.feature.settings.SettingsViewModel
import com.geely.ex2.tools.ui.clickableWithSystemSound
import com.geely.ex2.tools.ui.components.FlymeSettingsInfoItem
import com.geely.ex2.tools.ui.components.FlymeSettingsSection
import com.geely.ex2.tools.ui.components.GeelyTopAppBar
import com.geely.ex2.tools.ui.components.isFlymeRailCompact
import com.geely.ex2.tools.ui.theme.GeelyEx2ToolsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = remember { getAppVersionName(context) }
    var showDeveloperDialog by remember { mutableStateOf(false) }
    var vhalTestResult by remember { mutableStateOf("") }
    var vhalTestRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showDeveloperDialog) {
        DeveloperAboutDialog(onDismiss = { showDeveloperDialog = false })
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            GeelyTopAppBar(
                title = stringResource(R.string.settings_screen_title),
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
            FlymeSettingsSection(title = stringResource(R.string.settings_section_language)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_language_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_language_summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                )
                AppLocale.entries.forEachIndexed { index, locale ->
                    LanguageOptionRow(
                        title = stringResource(locale.labelRes),
                        selected = uiState.selectedLocale == locale,
                        onClick = { viewModel.onLocaleSelected(index) },
                        showDivider = index < AppLocale.entries.lastIndex,
                    )
                }
            }

            PhoneLinkSection()

            FlymeSettingsSection(title = stringResource(R.string.settings_section_about)) {
                FlymeSettingsInfoItem(
                    title = stringResource(R.string.settings_about_version),
                    summary = versionName.ifEmpty { stringResource(R.string.battery_app_widget_unavailable) },
                    modifier = Modifier.clickableWithSystemSound(
                        onClick = { showDeveloperDialog = true },
                    ),
                )
                FlymeSettingsInfoItem(
                    title = stringResource(R.string.settings_about_contact),
                    summary = "thang.dangvan7798@gmail.com",
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickableWithSystemSound(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
            )
        }
    }
}



@Preview(showBackground = true, name = "Settings Light")
@Composable
private fun SettingsScreenPreviewLight() {
    GeelyEx2ToolsTheme(darkTheme = false) {
        SettingsScreen(onBack = {})
    }
}

@Preview(showBackground = true, name = "Settings Dark")
@Composable
private fun SettingsScreenPreviewDark() {
    GeelyEx2ToolsTheme(darkTheme = true) {
        SettingsScreen(onBack = {})
    }
}
