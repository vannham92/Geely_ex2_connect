package com.geely.ex2.tools.feature.window.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geely.ex2.tools.R
import com.geely.ex2.tools.data.vhal.DoorCorner
import com.geely.ex2.tools.data.vhal.VhalConstants
import com.geely.ex2.tools.data.window.DoorWindowCornerState
import com.geely.ex2.tools.feature.window.AutoWindowViewModel
import com.geely.ex2.tools.ui.components.GeelyTopAppBar
import com.geely.ex2.tools.ui.components.TabVisibilityEffect
import com.geely.ex2.tools.ui.components.isFlymeRailCompact
import com.geely.ex2.tools.ui.theme.FlymeTheme
import com.geely.ex2.tools.ui.theme.GeelyEx2ToolsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoWindowScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AutoWindowViewModel = viewModel(),
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
                title = stringResource(R.string.auto_window_screen_title),
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Hàng trên: trước trái + trước phải
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WindowCornerCard(
                    corner = DoorCorner.FRONT_LEFT,
                    state = uiState.corners.firstOrNull { it.corner == DoorCorner.FRONT_LEFT },
                    checked = uiState.cornerEnabled[DoorCorner.FRONT_LEFT] == true,
                    onCheckedChange = { viewModel.onCornerEnabledChanged(DoorCorner.FRONT_LEFT, it) },
                    modifier = Modifier.weight(1f),
                )
                WindowCornerCard(
                    corner = DoorCorner.FRONT_RIGHT,
                    state = uiState.corners.firstOrNull { it.corner == DoorCorner.FRONT_RIGHT },
                    checked = uiState.cornerEnabled[DoorCorner.FRONT_RIGHT] == true,
                    onCheckedChange = { viewModel.onCornerEnabledChanged(DoorCorner.FRONT_RIGHT, it) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Hàng dưới: sau trái + sau phải
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WindowCornerCard(
                    corner = DoorCorner.REAR_LEFT,
                    state = uiState.corners.firstOrNull { it.corner == DoorCorner.REAR_LEFT },
                    checked = uiState.cornerEnabled[DoorCorner.REAR_LEFT] == true,
                    onCheckedChange = { viewModel.onCornerEnabledChanged(DoorCorner.REAR_LEFT, it) },
                    modifier = Modifier.weight(1f),
                )
                WindowCornerCard(
                    corner = DoorCorner.REAR_RIGHT,
                    state = uiState.corners.firstOrNull { it.corner == DoorCorner.REAR_RIGHT },
                    checked = uiState.cornerEnabled[DoorCorner.REAR_RIGHT] == true,
                    onCheckedChange = { viewModel.onCornerEnabledChanged(DoorCorner.REAR_RIGHT, it) },
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = stringResource(
                    R.string.auto_window_enable_summary,
                    VhalConstants.AUTO_WINDOW_DROP_PERCENT,
                    VhalConstants.AUTO_WINDOW_CLOSE_MAX_PERCENT,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )

            // Lưu ý cho dev: Điều khiển kính cần bản build system (quyền CONTROL_CAR_WINDOWS);
            // bản user chỉ xem được trạng thái. Xem R.string.auto_window_footer_system.
        }
    }
}

@Composable
private fun WindowCornerCard(
    corner: DoorCorner,
    state: DoorWindowCornerState?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(cornerLabelRes(corner)),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = FlymeTheme.extraColors.switchTrackOff,
                    uncheckedBorderColor = Color.Transparent,
                    checkedBorderColor = Color.Transparent,
                ),
            )
        }
        Text(
            text = cornerSummary(state),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun cornerLabelRes(corner: DoorCorner): Int = when (corner) {
    DoorCorner.FRONT_LEFT -> R.string.auto_window_corner_fl
    DoorCorner.FRONT_RIGHT -> R.string.auto_window_corner_fr
    DoorCorner.REAR_LEFT -> R.string.auto_window_corner_rl
    DoorCorner.REAR_RIGHT -> R.string.auto_window_corner_rr
}

@Composable
private fun cornerSummary(state: DoorWindowCornerState?): String {
    val doorText = when (state?.isDoorOpen) {
        true -> stringResource(R.string.auto_window_door_open)
        false -> stringResource(R.string.auto_window_door_closed)
        null -> stringResource(R.string.auto_window_value_unavailable)
    }
    val percent = state?.windowPercent
        ?: return stringResource(R.string.auto_window_corner_summary_no_window, doorText)
    return when (percent) {
        0 -> stringResource(R.string.auto_window_corner_summary_closed, doorText)
        100 -> stringResource(R.string.auto_window_corner_summary_fully_open, doorText)
        else -> stringResource(R.string.auto_window_corner_summary, doorText, percent)
    }
}

@Preview(showBackground = true, name = "Auto window Light")
@Composable
private fun AutoWindowScreenPreviewLight() {
    GeelyEx2ToolsTheme(darkTheme = false) {
        AutoWindowScreen(onBack = {})
    }
}

@Preview(showBackground = true, name = "Auto window Dark")
@Composable
private fun AutoWindowScreenPreviewDark() {
    GeelyEx2ToolsTheme(darkTheme = true) {
        AutoWindowScreen(onBack = {})
    }
}
