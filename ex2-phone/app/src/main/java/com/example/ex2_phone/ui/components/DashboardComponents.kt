package com.example.ex2_phone.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ex2_phone.data.network.TransportKind
import com.example.ex2_phone.ui.theme.Danger
import com.example.ex2_phone.ui.theme.Ink
import com.example.ex2_phone.ui.theme.StrokeSoft
import com.example.ex2_phone.ui.theme.Surface1
import com.example.ex2_phone.ui.theme.Surface2
import com.example.ex2_phone.ui.theme.SurfaceHi
import com.example.ex2_phone.ui.theme.TextHi
import com.example.ex2_phone.ui.theme.TextLow
import com.example.ex2_phone.ui.theme.TextMid

/** Ô icon bo tròn, nền tô nhẹ theo [tint]. Dùng làm đầu mục / đầu card. */
@Composable
fun IconChip(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

/** Tiêu đề mục: icon nhỏ tô màu nhấn + chữ. */
@Composable
fun SectionHeader(
    icon: ImageVector,
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(icon = icon, tint = accent, size = 30.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = TextHi,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

/** Thẻ số liệu: icon góc trên, số lớn + đơn vị, nhãn phía dưới. */
@Composable
fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Surface2, Surface1)))
            .border(1.dp, StrokeSoft, RoundedCornerShape(22.dp))
            .padding(16.dp),
    ) {
        IconChip(icon = icon, tint = color, size = 34.dp)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = TextHi,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = unit,
                color = TextMid,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Text(text = label, color = TextMid, fontSize = 13.sp)
    }
}

/** Số liệu gọn nằm ngang: icon + nhãn + giá trị. Dùng trong hero. */
@Composable
fun MiniStat(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconChip(icon = icon, tint = color, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = label, color = TextMid, fontSize = 12.sp)
            Text(
                text = value,
                color = TextHi,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            )
        }
    }
}

/** Vòng đo tròn (dùng cho pin). Số ở giữa, cung tô theo [color], có animation. */
@Composable
fun RingGauge(
    percent: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    strokeWidth: Dp = 11.dp,
    centerLabel: String = "",
) {
    val sweep by animateFloatAsState(
        targetValue = (percent.coerceIn(0f, 100f) / 100f) * 360f,
        animationSpec = tween(700),
        label = "ring_sweep"
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = androidx.compose.ui.geometry.Size(
                this.size.width - stroke,
                this.size.height - stroke
            )
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            // Rãnh nền
            drawArc(
                color = Color.White.copy(alpha = 0.07f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Cung giá trị
            drawArc(
                brush = Brush.sweepGradient(listOf(color.copy(alpha = 0.75f), color)),
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%.0f".format(percent),
                color = TextHi,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
            )
            if (centerLabel.isNotEmpty()) {
                Text(text = centerLabel, color = TextMid, fontSize = 12.sp)
            }
        }
    }
}

/** Viên nhãn (chip) nhỏ: nền tô nhẹ + chữ theo [color]. */
@Composable
fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text = text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Huy hiệu trạng thái kết nối kiểu pill. Chỉ hai trạng thái: đã kết nối hoặc mất kết nối —
 * không hiển thị "đang kết nối" (app tự thử lại ngầm).
 *
 * [transport] khác `null` thì nhãn/icon đổi theo ống dẫn đang dùng (Bluetooth hay WiFi).
 */
@Composable
fun StatusPill(
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    transport: TransportKind? = null,
) {
    val color by animateColorAsState(
        targetValue = if (isConnected) MaterialTheme.colorScheme.primary else Danger,
        animationSpec = tween(500),
        label = "status_color"
    )
    val text = when {
        !isConnected -> "Mất kết nối"
        transport == TransportKind.BLUETOOTH -> "Bluetooth"
        transport == TransportKind.WIFI -> "WiFi"
        else -> "Đã kết nối"
    }
    val icon = when {
        !isConnected -> Icons.Rounded.WifiOff
        transport == TransportKind.BLUETOOTH -> Icons.Rounded.Bluetooth
        else -> Icons.Rounded.Wifi
    }
    Pill(text = text, color = color, modifier = modifier, leadingIcon = icon)
}

/** Một lựa chọn của [SegmentedControl]. */
data class SegOption(val label: String, val icon: ImageVector? = null)

/**
 * Bộ chọn phân đoạn: các nút nằm trong một khay bo tròn; mục đang chọn tô màu nhấn.
 */
@Composable
fun SegmentedControl(
    options: List<SegOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    accent: Color,
    onAccent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .border(1.dp, StrokeSoft, RoundedCornerShape(18.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(
                if (selected) accent else Color.Transparent,
                tween(280),
                label = "seg_bg_$index"
            )
            val fg by animateColorAsState(
                if (selected) onAccent else TextMid,
                tween(280),
                label = "seg_fg_$index"
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .clickable(enabled = enabled) { onSelect(index) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (option.icon != null) {
                    Icon(option.icon, null, tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(text = option.label, color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

/** Card công tắc: icon + tiêu đề + trạng thái + [Switch]. Viền sáng lên khi bật. */
@Composable
fun ToggleCard(
    icon: ImageVector?,
    title: String,
    onText: String,
    offText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GlassCard(
        modifier = modifier,
        borderColor = if (checked) accent.copy(alpha = 0.45f) else StrokeSoft,
        glowColor = if (checked) accent else null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                IconChip(icon = icon, tint = if (checked) accent else TextMid, size = 42.dp)
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextHi,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (checked) onText else offText,
                    color = if (checked) accent else TextLow,
                    fontSize = 13.sp,
                )
            }
            // Màu nhấn sáng (Trắng, Be…) làm núm trắng biến mất vào rãnh → đổi núm sang màu tối.
            val lightAccent = accent.luminance() > 0.6f
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (lightAccent) Ink else Color.White,
                    checkedTrackColor = accent,
                    uncheckedThumbColor = TextMid,
                    uncheckedTrackColor = SurfaceHi,
                    uncheckedBorderColor = Color.Transparent,
                )
            )
        }
    }
}

/**
 * Card hành động: bấm một lần là chạy ngay, không có trạng thái bật/tắt (khác [ToggleCard]).
 * [tint] tô viền + icon (mỗi hành động một màu riêng); quầng sáng chỉ loé lên lúc nhấn.
 */
@Composable
fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val borderColor by animateColorAsState(
        targetValue = tint.copy(alpha = if (pressed) 0.85f else 0.45f),
        animationSpec = tween(180),
        label = "actionBorder",
    )

    GlassCard(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        ),
        borderColor = borderColor,
        glowColor = if (pressed) tint else null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconChip(icon = icon, tint = tint, size = 42.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextHi,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = subtitle,
                    color = TextLow,
                    fontSize = 13.sp,
                )
            }
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
