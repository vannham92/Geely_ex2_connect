package com.example.ex2_phone.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ex2_phone.ui.components.ActionCard
import com.example.ex2_phone.ui.components.GlassCard
import com.example.ex2_phone.ui.components.IconChip
import com.example.ex2_phone.ui.components.SectionHeader
import com.example.ex2_phone.ui.components.SegOption
import com.example.ex2_phone.ui.components.SegmentedControl
import com.example.ex2_phone.ui.components.ToggleCard
import com.example.ex2_phone.ui.theme.Ink
import com.example.ex2_phone.ui.theme.QuickCoolBlue
import com.example.ex2_phone.ui.theme.QuickWarmRust
import com.example.ex2_phone.ui.theme.StrokeSoft
import com.example.ex2_phone.ui.theme.Surface1
import com.example.ex2_phone.ui.theme.TextHi
import com.example.ex2_phone.ui.theme.TextMid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HvacScreen(
    viewModel: CarViewModel,
    carColor: CarColor,
) {
    val carStatus by viewModel.carStatus.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val accent by animateColorAsState(carColor.accent, tween(450), label = "accent")
    val bgTop = lerp(Ink, carColor.accent, 0.08f)

    val available = carStatus.hvacAvailable

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
            Text(text = "Điều hoà", color = TextHi, fontSize = 28.sp,
                fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
            Text(text = "Nhiệt độ, quạt gió và các chế độ", color = TextMid, fontSize = 14.sp)

            Spacer(Modifier.height(20.dp))

            if (!available) {
                Spacer(Modifier.height(40.dp))
                Text(
                    text = "Hệ thống điều hoà chưa sẵn sàng.\nCần khởi động xe hoặc chờ hệ thống khởi động.",
                    color = TextMid, fontSize = 14.sp, lineHeight = 20.sp,
                )
            }

            Column(modifier = Modifier.alpha(if (available) 1f else 0.5f)) {
                // AC
                SectionHeader(icon = Icons.Rounded.AcUnit, title = "Máy lạnh (A/C)", accent = accent)
                Spacer(Modifier.height(12.dp))
                ToggleCard(
                    icon = null,
                    title = "A/C",
                    onText = "Đang bật",
                    offText = "Đang tắt",
                    checked = carStatus.hvacAcOn,
                    onCheckedChange = { if (available) viewModel.setHvacAc(it) },
                    accent = accent,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = available,
                )
                Spacer(Modifier.height(24.dp))

                // Nhiệt độ
                SectionHeader(icon = Icons.Rounded.Thermostat, title = "Nhiệt độ ghế lái", accent = accent)
                Spacer(Modifier.height(12.dp))
                StepperCard(
                    icon = null,
                    title = "Nhiệt độ",
                    valueText = if (available) "%.0f°C".format(carStatus.hvacTempC) else "--°C",
                    onDecrease = { if (available) viewModel.setHvacTemp(carStatus.hvacTempC - 1f) },
                    onIncrease = { if (available) viewModel.setHvacTemp(carStatus.hvacTempC + 1f) },
                    accent = accent,
                )
                Spacer(Modifier.height(24.dp))

                // Quạt gió
                SectionHeader(icon = Icons.Rounded.Air, title = "Quạt gió", accent = accent)
                Spacer(Modifier.height(12.dp))
                StepperCard(
                    icon = null,
                    title = "Mức quạt",
                    valueText = if (available) "${carStatus.hvacFanSpeed}" else "--",
                    onDecrease = { if (available && carStatus.hvacFanSpeed > 1) viewModel.setHvacFan(carStatus.hvacFanSpeed - 1) },
                    onIncrease = { if (available && carStatus.hvacFanSpeed < 8) viewModel.setHvacFan(carStatus.hvacFanSpeed + 1) },
                    accent = accent,
                )
                Spacer(Modifier.height(24.dp))

                // Tuần hoàn gió — chọn gió ngoài / gió trong
                SectionHeader(icon = Icons.Rounded.Cached, title = "Tuần hoàn gió", accent = accent)
                Spacer(Modifier.height(14.dp))
                SegmentedControl(
                    options = listOf(
                        SegOption("Gió ngoài", Icons.Rounded.Air),
                        SegOption("Gió trong", Icons.Rounded.Cached),
                    ),
                    selectedIndex = if (carStatus.hvacRecircOn) 1 else 0,
                    onSelect = { if (available) viewModel.setHvacRecirc(it == 1) },
                    accent = accent,
                    onAccent = carColor.onAccent,
                    enabled = available,
                )
                Spacer(Modifier.height(24.dp))

                // Chế độ nhanh — bấm một lần là đặt luôn nhiệt độ + quạt, không phải công tắc
                SectionHeader(icon = Icons.Rounded.Bolt, title = "Chế độ nhanh", accent = accent)
                Spacer(Modifier.height(12.dp))
                ActionCard(
                    icon = Icons.Rounded.AcUnit,
                    title = "Làm mát nhanh",
                    subtitle = "Đặt 18°C, quạt mức 7",
                    tint = QuickCoolBlue,
                    onClick = { if (available) viewModel.quickCool() },
                    enabled = available,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                ActionCard(
                    icon = Icons.Rounded.LocalFireDepartment,
                    title = "Làm ấm nhanh",
                    subtitle = "Đặt 31°C, quạt mức 7",
                    tint = QuickWarmRust,
                    onClick = { if (available) viewModel.quickWarm() },
                    enabled = available,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StepperCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    title: String,
    valueText: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    accent: androidx.compose.ui.graphics.Color,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = StrokeSoft,
        glowColor = null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                IconChip(icon = icon, tint = accent, size = 42.dp)
                Spacer(Modifier.width(14.dp))
            }
            Text(
                text = title,
                color = TextHi,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = onDecrease,
                    modifier = Modifier.background(Surface1, shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Giảm", tint = TextHi)
                }
                Text(
                    text = valueText,
                    color = accent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = 48.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                IconButton(
                    onClick = onIncrease,
                    modifier = Modifier.background(Surface1, shape = RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tăng", tint = TextHi)
                }
            }
        }
    }
}
