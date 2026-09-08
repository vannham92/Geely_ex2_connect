package com.example.ex2_phone.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.example.ex2_phone.ui.components.SectionHeader
import com.example.ex2_phone.ui.components.SegOption
import com.example.ex2_phone.ui.components.SegmentedControl
import com.example.ex2_phone.ui.components.ToggleCard
import com.example.ex2_phone.ui.components.GlassCard
import com.example.ex2_phone.ui.theme.Ink

import com.example.ex2_phone.ui.theme.Surface1
import com.example.ex2_phone.ui.theme.TextHi
import com.example.ex2_phone.ui.theme.TextMid
import com.example.ex2_phone.ui.theme.TextLow

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    viewModel: CarViewModel,
    carColor: CarColor,
) {
    val carStatus by viewModel.carStatus.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val accent by animateColorAsState(carColor.accent, tween(450), label = "accent")
    val bgTop = lerp(Ink, carColor.accent, 0.08f)

    val noneAvailable = !carStatus.ambientLightAvailable && !carStatus.wifiAvailable &&
        !carStatus.sceneAvailable && !carStatus.windowAvailable

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
        Text(text = "Điều khiển", color = TextHi, fontSize = 28.sp,
            fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
        Text(text = "Ánh sáng, kết nối, cửa xe", color = TextMid, fontSize = 14.sp)

        Spacer(Modifier.height(20.dp))

        // Chưa kết nối / xe chưa báo khả dụng: vẫn hiện đầy đủ các mục, chỉ làm mờ và khoá thao tác
        if (noneAvailable) {
            Text(
                text = "Chưa có điều khiển nào khả dụng.\nKết nối tới xe để bật các tính năng.",
                color = TextMid, fontSize = 14.sp, lineHeight = 20.sp,
            )
            Spacer(Modifier.height(20.dp))
        }

        // Cửa kính
        val windowAvailable = carStatus.windowAvailable
        Column(modifier = Modifier.alpha(if (windowAvailable) 1f else 0.5f)) {
            SectionHeader(
                icon = Icons.Rounded.Tune,
                title = "Cửa kính",
                accent = accent,
            )
            Spacer(Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Nút nhanh: Đóng tất cả / Mở tất cả
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setWindow(0, 0) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            enabled = windowAvailable,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                disabledContainerColor = accent,
                            ),
                        ) {
                            Text("Đóng tất cả", color = carColor.onAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Button(
                            onClick = { viewModel.setWindow(0, 100) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            enabled = windowAvailable,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                disabledContainerColor = Color.White.copy(alpha = 0.1f),
                            ),
                        ) {
                            Text("Mở tất cả", color = TextHi, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Kéo núm xuống để hạ kính, kéo lên để đóng",
                        color = TextLow, fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(12.dp))

                    // Bố trí 2×2 đúng vị trí trên xe: hàng trên = ghế trước, trái ↔ phải
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WindowTile(
                            label = "Trước trái",
                            percent = carStatus.windowFlPercent,
                            accent = accent,
                            enabled = windowAvailable,
                            onPercentCommit = { viewModel.setWindow(1, it) },
                            modifier = Modifier.weight(1f),
                        )
                        WindowTile(
                            label = "Trước phải",
                            percent = carStatus.windowFrPercent,
                            accent = accent,
                            enabled = windowAvailable,
                            onPercentCommit = { viewModel.setWindow(2, it) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WindowTile(
                            label = "Sau trái",
                            percent = carStatus.windowRlPercent,
                            accent = accent,
                            enabled = windowAvailable,
                            onPercentCommit = { viewModel.setWindow(3, it) },
                            modifier = Modifier.weight(1f),
                        )
                        WindowTile(
                            label = "Sau phải",
                            percent = carStatus.windowRrPercent,
                            accent = accent,
                            enabled = windowAvailable,
                            onPercentCommit = { viewModel.setWindow(4, it) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        // Đèn viền nội thất
        val ambientAvailable = carStatus.ambientLightAvailable
        Column(modifier = Modifier.alpha(if (ambientAvailable) 1f else 0.5f)) {
            SectionHeader(icon = Icons.Rounded.Lightbulb, title = "Đèn viền nội thất", accent = accent)
            Spacer(Modifier.height(12.dp))
            ToggleCard(
                icon = Icons.Rounded.Lightbulb,
                title = "Đèn Ambient",
                onText = "Đang bật",
                offText = "Đang tắt",
                checked = carStatus.ambientLightOn,
                onCheckedChange = { if (ambientAvailable) viewModel.setAmbientLight(it) },
                accent = accent,
                modifier = Modifier.fillMaxWidth(),
                enabled = ambientAvailable,
            )
        }
        Spacer(Modifier.height(24.dp))

        // Chế độ cảnh (Scene Mode)
        val sceneAvailable = carStatus.sceneAvailable
        Column(modifier = Modifier.alpha(if (sceneAvailable) 1f else 0.5f)) {
            SectionHeader(icon = Icons.Rounded.Weekend, title = "Chế độ thư giãn", accent = accent)
            Spacer(Modifier.height(14.dp))
            SegmentedControl(
                options = listOf(
                    SegOption("Nghỉ ngơi"),
                    SegOption("Cắm trại")
                ),
                selectedIndex = carStatus.sceneMode - 1,
                onSelect = { if (sceneAvailable) viewModel.setScene(it + 1) },
                accent = accent,
                onAccent = carColor.onAccent,
                enabled = sceneAvailable,
            )
        }
        Spacer(Modifier.height(24.dp))

        // WiFi / hotspot
        val wifiAvailable = carStatus.wifiAvailable
        Column(modifier = Modifier.alpha(if (wifiAvailable) 1f else 0.5f)) {
            SectionHeader(icon = Icons.Rounded.Wifi, title = "WiFi / Hotspot", accent = accent)
            Spacer(Modifier.height(12.dp))
            ToggleCard(
                icon = Icons.Rounded.Wifi,
                title = "WiFi",
                onText = carStatus.wifiIp?.let { "Đang bật · $it" } ?: "Đang bật",
                offText = "Đang tắt",
                checked = carStatus.wifiOn,
                onCheckedChange = { if (wifiAvailable) viewModel.setWifi(it) },
                accent = accent,
                modifier = Modifier.fillMaxWidth(),
                enabled = wifiAvailable,
            )
        }
        Spacer(Modifier.height(24.dp))
        }
    }
}

private val WindowTrackHeight = 150.dp

/** Rãnh rộng đúng bằng núm — núm lấp kín bề ngang, không lọt thỏm giữa rãnh. */
private val WindowThumbSize = 40.dp
private val WindowTrackWidth = WindowThumbSize

/**
 * Ô điều khiển 1 cửa kính dạng slider **dọc**, đi theo chiều kính thật: núm ở đỉnh = đóng,
 * kéo xuống = hạ kính. Phần tô màu từ đỉnh xuống tới núm = khoảng kính đã hạ.
 *
 * [percent] theo protocol = **độ mở** (0 = đóng, 100 = mở hết). Số hiện ra cho người dùng là
 * **độ đóng** (`100 - percent`), nên đóng kín hiện `100%` — đảo đúng một chỗ ở `statusText`,
 * giá trị gửi xe giữ nguyên thang protocol.
 *
 * Kéo/chạm → cập nhật ngay trên phone (optimistic), thả tay → gửi lệnh cho xe.
 * Ngoài lúc kéo thì bám theo giá trị ViewModel — vốn đã latch đích trong lúc kính chạy,
 * nên các frame status trung gian (mở 90% nhưng đang ở 40%) không kéo UI ngược lại.
 */
@Composable
private fun WindowTile(
    label: String,
    percent: Int,
    accent: Color,
    enabled: Boolean,
    onPercentCommit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Khi đang kéo dùng local state, khi không kéo bám theo giá trị từ xe
    var isDragging by remember { mutableStateOf(false) }
    var localPercent by remember { mutableFloatStateOf(percent.toFloat()) }
    LaunchedEffect(percent, isDragging) {
        if (!isDragging) localPercent = percent.toFloat()
    }

    val current = localPercent.roundToInt()
    val statusText = when (current) {
        0 -> "Đóng"
        100 -> "Mở hết"
        else -> "${100 - current}%"   // hiện độ đóng, xem KDoc
    }

    // Kéo tay thì bám 1:1, còn xe báo số mới thì trượt mượt tới đó
    val shownPercent by animateFloatAsState(
        targetValue = localPercent,
        animationSpec = if (isDragging) snap() else tween(400),
        label = "windowPercent",
    )

    val density = LocalDensity.current
    val thumbPx = with(density) { WindowThumbSize.toPx() }
    var trackPx by remember { mutableFloatStateOf(with(density) { WindowTrackHeight.toPx() }) }

    // y ở đỉnh = kính đóng (percent 0), y ở đáy = mở hết (percent 100) — cùng chiều kính hạ xuống.
    fun percentFromY(y: Float): Float {
        val travel = trackPx - thumbPx
        if (travel <= 0f) return localPercent
        return (((y - thumbPx / 2f) / travel) * 100f).coerceIn(0f, 100f)
    }

    val fraction = shownPercent / 100f
    val thumbColor = if (enabled) accent else TextLow

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextMid, fontSize = 13.sp)
        Text(
            text = statusText,
            color = if (current == 0) TextLow else TextHi,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .width(WindowTrackWidth)
                .height(WindowTrackHeight)
                .clip(RoundedCornerShape(WindowTrackWidth / 2))
                .background(Surface1)
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(WindowTrackWidth / 2))
                .onSizeChanged { trackPx = it.height.toFloat() }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val p = percentFromY(offset.y)
                        localPercent = p
                        onPercentCommit(p.roundToInt())
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            localPercent = percentFromY(offset.y)
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            localPercent = percentFromY(change.position.y)
                        },
                        onDragEnd = {
                            isDragging = false
                            onPercentCommit(localPercent.roundToInt())
                        },
                        onDragCancel = { isDragging = false },
                    )
                },
        ) {
            // Khoảng kính đã hạ, tô từ đỉnh xuống tới núm
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(WindowTrackHeight * fraction)
                    .background(accent.copy(alpha = if (enabled) 0.40f else 0.15f)),
            )
            // Núm tròn: đóng ở đỉnh, mở hết ở đáy
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, (fraction * (trackPx - thumbPx)).roundToInt()) }
                    .size(WindowThumbSize)
                    .clip(CircleShape)
                    .background(thumbColor),
            )
        }
    }
}
