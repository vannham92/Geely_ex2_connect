package com.example.ex2_phone.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel

private val BatteryGreen = Color(0xFF00E676)

@Composable
fun DashboardScreen(viewModel: CarViewModel = viewModel()) {
    val carStatus by viewModel.carStatus.collectAsState()
    val isConnected by viewModel.connected.collectAsState()
    val isError by viewModel.connectionError.collectAsState()

    var ipAddress by remember { mutableStateOf("10.77.86.7") }
    var carColor by rememberSaveable { mutableStateOf(CarColor.DEFAULT) }
    var showColorPicker by remember { mutableStateOf(false) }

    // Màu chủ đạo đổi mượt khi chọn màu xe khác
    val accent by animateColorAsState(carColor.accent, tween(450), label = "accent")
    val bgTop = lerp(Color(0xFF141414), carColor.accent, 0.12f)

    LaunchedEffect(Unit) {
        viewModel.connect(ipAddress)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, Color(0xFF0A0A0A))))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: tiêu đề + nút cài đặt
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Geely EX2",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Remote Control",
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(
                onClick = { showColorPicker = true },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Cài đặt màu xe",
                    tint = accent
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hàng: trạng thái kết nối + pill màu xe hiện tại
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ConnectionStatusBadge(isConnected = isConnected, isError = isError)
            CarColorPill(carColor = carColor, onClick = { showColorPicker = true })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                label = { Text("Car IP Address", color = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = accent,
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = accent
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = { viewModel.connect(ipAddress) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("Connect", color = carColor.onAccent, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isError && !isConnected) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1414)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Không thể kết nối WebSocket. Đang thử lại...\nĐảm bảo xe cùng mạng WiFi và app xe đang mở.",
                    color = Color(0xFFFF6666),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Speed & Battery
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardCard(
                title = "Tốc độ",
                value = "%.0f".format(carStatus.speedKmh),
                unit = "km/h",
                color = accent,
                modifier = Modifier.weight(1f)
            )
            DashboardCard(
                title = "Pin",
                value = "%.1f".format(carStatus.batteryPercent),
                unit = "%",
                color = BatteryGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Chế độ lái",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(14.dp))

        val currentMode = carStatus.drivingMode
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DrivingModeButton(
                title = "Eco",
                isActive = currentMode == 1,
                accent = accent,
                onAccent = carColor.onAccent,
                onClick = { viewModel.setDrivingMode(1) },
                modifier = Modifier.weight(1f)
            )
            DrivingModeButton(
                title = "Comfort",
                isActive = currentMode == 2,
                accent = accent,
                onAccent = carColor.onAccent,
                onClick = { viewModel.setDrivingMode(2) },
                modifier = Modifier.weight(1f)
            )
            DrivingModeButton(
                title = "Sport",
                isActive = currentMode == 3,
                accent = accent,
                onAccent = carColor.onAccent,
                onClick = { viewModel.setDrivingMode(3) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showColorPicker) {
        CarColorPickerDialog(
            selected = carColor,
            onSelect = { carColor = it },
            onDismiss = { showColorPicker = false }
        )
    }
}

@Composable
fun ConnectionStatusBadge(isConnected: Boolean, isError: Boolean) {
    val dotColor by animateColorAsState(
        targetValue = when {
            isConnected -> Color(0xFF00E676)
            isError -> Color(0xFFFF4444)
            else -> Color(0xFFFFAA00)
        },
        animationSpec = tween(500),
        label = "dot_color"
    )
    val label = when {
        isConnected -> "Connected"
        isError -> "Disconnected"
        else -> "Connecting..."
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = dotColor, fontSize = 13.sp)
    }
}

/** Pill nhỏ hiển thị màu xe đang chọn, bấm vào để mở bảng chọn màu. */
@Composable
fun CarColorPill(carColor: CarColor, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(carColor.swatch)
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = carColor.label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun DashboardCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C1C1E))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, color = Color.Gray, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, color = color, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = unit,
                color = color.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}

@Composable
fun DrivingModeButton(
    title: String,
    isActive: Boolean,
    accent: Color,
    onAccent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        if (isActive) accent else Color(0xFF2A2A2C),
        tween(300),
        label = "mode_bg"
    )
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        border = if (isActive) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = title,
            color = if (isActive) onAccent else Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Bảng chọn màu xe: 6 màu, chọn xong đổi màu chủ đạo cả app. */
@Composable
fun CarColorPickerDialog(
    selected: CarColor,
    onSelect: (CarColor) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1C1C1E))
                .border(1.dp, selected.accent.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "Màu xe",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Chọn màu để đổi màu chủ đạo giao diện",
                color = Color.Gray,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Lưới 2 hàng x 3 màu
            CarColor.entries.chunked(3).forEach { rowColors ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowColors.forEach { color ->
                        ColorSwatch(
                            carColor = color,
                            isSelected = color == selected,
                            onClick = { onSelect(color) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = selected.accent)
            ) {
                Text("Xong", color = selected.onAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ColorSwatch(
    carColor: CarColor,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(carColor.swatch)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) carColor.accent else Color.White.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = carColor.onSwatch
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = carColor.label,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
