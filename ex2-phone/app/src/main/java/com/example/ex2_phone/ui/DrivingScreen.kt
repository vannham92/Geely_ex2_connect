package com.example.ex2_phone.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ex2_phone.ui.components.SectionHeader
import com.example.ex2_phone.ui.components.SegOption
import com.example.ex2_phone.ui.components.SegmentedControl
import com.example.ex2_phone.ui.components.ToggleCard
import com.example.ex2_phone.ui.theme.Ink
import com.example.ex2_phone.ui.theme.TextHi
import com.example.ex2_phone.ui.theme.TextMid

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrivingScreen(
    viewModel: CarViewModel,
    carColor: CarColor,
) {
    val carStatus by viewModel.carStatus.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val accent by animateColorAsState(carColor.accent, tween(450), label = "accent")
    val bgTop = lerp(Ink, carColor.accent, 0.08f)

    val noneAvailable = !carStatus.drivingModeAvailable && !carStatus.regenAvailable && !carStatus.avasAvailable

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.requestStatus() },
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, Ink)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
        ) {
        Text(text = "Lái xe", color = TextHi, fontSize = 28.sp,
            fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
        Text(text = "Chế độ lái, năng lượng và âm thanh", color = TextMid, fontSize = 14.sp)

        Spacer(Modifier.height(20.dp))

        // Chưa kết nối / xe chưa báo khả dụng: vẫn hiện đầy đủ các mục, chỉ làm mờ và khoá thao tác
        if (noneAvailable) {
            Text(
                text = "Chưa có điều khiển lái xe nào khả dụng.\nKết nối tới xe để bật các tính năng.",
                color = TextMid, fontSize = 14.sp, lineHeight = 20.sp,
            )
            Spacer(Modifier.height(20.dp))
        }

        // Chế độ lái
        val drivingModeAvailable = carStatus.drivingModeAvailable
        Column(modifier = Modifier.alpha(if (drivingModeAvailable) 1f else 0.5f)) {
            SectionHeader(icon = Icons.Rounded.Tune, title = "Chế độ lái", accent = accent)
            Spacer(Modifier.height(14.dp))
            SegmentedControl(
                options = listOf(
                    SegOption("Eco", Icons.Rounded.Eco),
                    SegOption("Comfort", Icons.Rounded.Weekend),
                    SegOption("Sport", Icons.Rounded.Bolt),
                ),
                selectedIndex = carStatus.drivingMode - 1,
                onSelect = { if (drivingModeAvailable) viewModel.setDrivingMode(it + 1) },
                accent = accent,
                onAccent = carColor.onAccent,
                enabled = drivingModeAvailable,
            )
        }
        Spacer(Modifier.height(24.dp))

        // Thu hồi năng lượng
        val regenAvailable = carStatus.regenAvailable
        Column(modifier = Modifier.alpha(if (regenAvailable) 1f else 0.5f)) {
            SectionHeader(icon = Icons.Rounded.BatteryChargingFull, title = "Thu hồi năng lượng", accent = accent)
            Spacer(Modifier.height(14.dp))
            SegmentedControl(
                options = listOf(
                    SegOption("Thấp"),
                    SegOption("Trung bình"),
                    SegOption("Cao"),
                ),
                selectedIndex = carStatus.regenLevel - 1,
                onSelect = { if (regenAvailable) viewModel.setRegenLevel(it + 1) },
                accent = accent,
                onAccent = carColor.onAccent,
                enabled = regenAvailable,
            )
        }
        Spacer(Modifier.height(24.dp))

        // AVAS
        val avasAvailable = carStatus.avasAvailable
        Column(modifier = Modifier.alpha(if (avasAvailable) 1f else 0.5f)) {
            SectionHeader(icon = Icons.Rounded.VolumeUp, title = "Âm thanh cảnh báo (dưới 30km/h)", accent = accent)
            Spacer(Modifier.height(12.dp))
            ToggleCard(
                icon = if (carStatus.avasMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                title = "AVAS",
                onText = "Đang phát âm thanh",
                offText = "Đã tắt tiếng",
                checked = !carStatus.avasMuted,
                onCheckedChange = { if (avasAvailable) viewModel.setAvasMute(!it) },
                accent = accent,
                modifier = Modifier.fillMaxWidth(),
                enabled = avasAvailable,
            )
        }
        Spacer(Modifier.height(24.dp))
        }
    }
}
