package com.example.ex2_phone.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiOff
import kotlin.math.roundToInt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.ex2_phone.data.network.CarStatusMessage
import androidx.compose.ui.unit.sp
import com.example.ex2_phone.ui.components.GlassCard
import com.example.ex2_phone.ui.components.MiniStat
import com.example.ex2_phone.ui.components.RingGauge
import com.example.ex2_phone.ui.components.SectionHeader
import com.example.ex2_phone.ui.components.StatTile
import com.example.ex2_phone.ui.components.StatusPill
import com.example.ex2_phone.ui.theme.BatteryGreen
import com.example.ex2_phone.ui.theme.ComfortBlue
import com.example.ex2_phone.ui.theme.Danger
import com.example.ex2_phone.ui.theme.Warn
import com.example.ex2_phone.ui.theme.Ink
import com.example.ex2_phone.ui.theme.TextHi
import com.example.ex2_phone.ui.theme.TextLow
import com.example.ex2_phone.ui.theme.TextMid

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: CarViewModel,
    carColor: CarColor,
    ipAddress: String,
    onIpChange: (String) -> Unit,
    onConnect: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHvac: () -> Unit,
) {
    val carStatus by viewModel.carStatus.collectAsState()
    val isConnected by viewModel.connected.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val activeTransport by viewModel.activeTransport.collectAsState()


    val accent by animateColorAsState(carColor.accent, tween(450), label = "accent")
    val bgTop = lerp(Ink, carColor.accent, 0.10f)



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
                .padding(top = 8.dp, bottom = 28.dp),
        ) {
        // --- Thanh trên: thương hiệu + trạng thái kết nối ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Geely EX2", color = TextHi, fontSize = 28.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = (-0.8).sp)
                Text(text = "Điều khiển từ xa", color = accent, fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            }
            StatusPill(isConnected = isConnected, transport = activeTransport)
        }

        Spacer(Modifier.height(16.dp))

        // --- Cảnh báo chưa kết nối: hướng dẫn sang Cài đặt ---
        if (!isConnected) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSettings),
                borderColor = Warn.copy(alpha = 0.35f),
                glowColor = Warn,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "Cảnh báo",
                        tint = Warn,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chưa kết nối với xe",
                            color = TextHi,
                            fontSize = 15.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        )
                        Text(
                            text = "Nhấn vào đây để mở Cài đặt và thiết lập kết nối",
                            color = TextMid,
                            fontSize = 13.sp,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = TextLow,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // --- Hero: ảnh xe + vòng pin + số liệu nhanh ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = accent.copy(alpha = 0.18f),
            glowColor = accent,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Trạng thái xe", color = TextMid, fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                }

                // Ảnh xe đổi theo màu sơn đã chọn trong Cài đặt
                Crossfade(targetState = carColor, animationSpec = tween(450), label = "carImage") { color ->
                    Image(
                        painter = painterResource(id = color.imageRes),
                        contentDescription = "Geely EX2 màu ${color.label}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RingGauge(
                        percent = if (carStatus.batteryAvailable) carStatus.batteryPercent else 0f,
                        color = BatteryGreen,
                        size = 132.dp,
                        centerLabel = "% Pin",
                    )
                    Spacer(Modifier.width(20.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MiniStat(
                            icon = Icons.Rounded.Route,
                            label = "Còn đi được",
                            value = if (carStatus.rangeAvailable) "${carStatus.rangeKm.roundToInt()} km" else "—",
                            color = accent,
                        )
                        MiniStat(
                            icon = Icons.Rounded.Route,
                            label = "Odometer",
                            value = if (carStatus.odometerAvailable) "%,d km".format(carStatus.odometerKm.toInt()) else "—",
                            color = TextMid,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // --- Thẻ số liệu ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatTile(
                icon = if (carStatus.wifiOn) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                label = "WiFi",
                value = when {
                    !carStatus.wifiAvailable -> "—"
                    carStatus.wifiOn -> "Bật"
                    else -> "Tắt"
                },
                unit = "",
                color = if (carStatus.wifiOn) BatteryGreen else TextMid,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                icon = Icons.Rounded.Thermostat,
                label = "Ngoài trời",
                value = if (carStatus.tempAvailable) "%.0f".format(carStatus.outsideTempC) else "—",
                unit = if (carStatus.tempAvailable) "°C" else "",
                color = ComfortBlue,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Tạm thời ẩn hiển thị Tình trạng lốp
        // TirePressureSection(carStatus, accent)
        // Spacer(Modifier.height(24.dp))

        // --- Chi tiết trạng thái (chỉ hiển thị) ---
        SectionHeader(icon = Icons.Rounded.Info, title = "Chi tiết", accent = accent)
        Spacer(Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = accent.copy(alpha = 0.12f)) {
            // Luôn hiện đủ các mục; mục xe chưa báo khả dụng thì mờ đi và để giá trị "—"
            Column(modifier = Modifier.fillMaxWidth()) {
                InfoRow(
                    label = "WiFi",
                    value = when {
                        !carStatus.wifiAvailable -> "—"
                        carStatus.wifiOn -> carStatus.wifiIp ?: "Đang bật"
                        else -> "Đang tắt"
                    },
                    valueColor = if (carStatus.wifiAvailable && carStatus.wifiOn) accent else TextMid,
                    available = carStatus.wifiAvailable,
                    onClick = onOpenSettings,
                )
                InfoDivider()
                InfoRow(
                    label = "Tuần hoàn gió",
                    value = when {
                        !carStatus.hvacAvailable -> "—"
                        carStatus.hvacRecircOn -> "Trong"
                        else -> "Ngoài"
                    },
                    valueColor = if (carStatus.hvacAvailable) TextHi else TextMid,
                    available = carStatus.hvacAvailable,
                    onClick = onOpenHvac,
                )
            }
        }
        }
    }
}

/** Một dòng thông tin: nhãn trái, giá trị phải. [onClick] có thì cả dòng bấm được để mở tab liên quan. */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    available: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (available) 1f else 0.5f)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMid, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, color = valueColor, fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            if (onClick != null) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = TextLow,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun InfoDivider() {
    androidx.compose.material3.HorizontalDivider(color = com.example.ex2_phone.ui.theme.StrokeSoft)
}

@Composable
private fun TirePressureSection(carStatus: CarStatusMessage, accent: androidx.compose.ui.graphics.Color) {
    SectionHeader(icon = androidx.compose.material.icons.Icons.Rounded.Info, title = "Tình trạng lốp", accent = accent)
    Spacer(Modifier.height(12.dp))
    GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = accent.copy(alpha = 0.12f)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TireItem("Trước Trái", carStatus.tirePressureAvailable, carStatus.tireFlLow)
                TireItem("Trước Phải", carStatus.tirePressureAvailable, carStatus.tireFrLow)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TireItem("Sau Trái", carStatus.tirePressureAvailable, carStatus.tireRlLow)
                TireItem("Sau Phải", carStatus.tirePressureAvailable, carStatus.tireRrLow)
            }
        }
    }
}

@Composable
private fun TireItem(label: String, isAvailable: Boolean, isLow: Boolean) {
    val color = if (!isAvailable) TextMid else if (isLow) Danger else com.example.ex2_phone.ui.theme.EcoGreen
    val statusText = if (!isAvailable) "—" else if (isLow) "Non hơi" else "OK"
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextMid, fontSize = 12.sp)
        Text(
            text = statusText,
            color = color,
            fontSize = 18.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}
