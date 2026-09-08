package com.geely.ex2.tools.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geely.ex2.tools.R
import com.geely.ex2.tools.navigation.AppRoutes
import com.geely.ex2.tools.ui.components.FlymeSettingsNavRow
import com.geely.ex2.tools.ui.components.GeelyTopAppBar
import com.geely.ex2.tools.ui.rememberOnClickWithSystemSound
import com.geely.ex2.tools.ui.theme.GeelyEx2ToolsTheme

data class ToolItem(
    val route: String,
    val titleRes: Int,
    val descriptionRes: Int,
)

private val tools = listOf(
    ToolItem(
        route = AppRoutes.WIFI,
        titleRes = R.string.tool_wifi_title,
        descriptionRes = R.string.tool_wifi_description,
    ),
    ToolItem(
        route = AppRoutes.TEMPERATURE,
        titleRes = R.string.tool_temperature_title,
        descriptionRes = R.string.tool_temperature_description,
    ),
    ToolItem(
        route = AppRoutes.SPEED,
        titleRes = R.string.tool_speed_title,
        descriptionRes = R.string.tool_speed_description,
    ),
    ToolItem(
        route = AppRoutes.DRIVING,
        titleRes = R.string.tool_driving_title,
        descriptionRes = R.string.tool_driving_description,
    ),
    ToolItem(
        route = AppRoutes.BATTERY,
        titleRes = R.string.tool_battery_title,
        descriptionRes = R.string.tool_battery_description,
    ),
    ToolItem(
        route = AppRoutes.AMBIENT_LIGHT,
        titleRes = R.string.tool_ambient_light_title,
        descriptionRes = R.string.tool_ambient_light_description,
    ),
    ToolItem(
        route = AppRoutes.AUTO_WINDOW,
        titleRes = R.string.tool_auto_window_title,
        descriptionRes = R.string.tool_auto_window_description,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAboutDialog by remember { mutableStateOf(false) }
    val onAboutClickWithSound = rememberOnClickWithSystemSound { showAboutDialog = true }

    if (showAboutDialog) {
        AppAboutDialog(onDismiss = { showAboutDialog = false })
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            GeelyTopAppBar(
                title = stringResource(R.string.home_tools_title),
                actions = {
                    IconButton(
                        onClick = onAboutClickWithSound,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = stringResource(R.string.home_about_content_description),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_brand_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(tools) { tool ->
                FlymeSettingsNavRow(
                    title = stringResource(tool.titleRes),
                    summary = stringResource(tool.descriptionRes),
                    onClick = { onToolClick(tool.route) },
                )
            }
            item {
                Text(
                    text = stringResource(R.string.home_access_footer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 12.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Home Light")
@Composable
private fun HomeScreenPreviewLight() {
    GeelyEx2ToolsTheme(darkTheme = false) {
        HomeScreen(onToolClick = {})
    }
}

@Preview(showBackground = true, name = "Home Dark")
@Composable
private fun HomeScreenPreviewDark() {
    GeelyEx2ToolsTheme(darkTheme = true) {
        HomeScreen(onToolClick = {})
    }
}
