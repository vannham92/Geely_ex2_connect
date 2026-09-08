package com.example.ex2_phone.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.ex2_phone.data.network.TransportMode
import com.example.ex2_phone.ui.components.ConnectCard
import com.example.ex2_phone.ui.components.GlassCard
import com.example.ex2_phone.ui.components.SectionHeader
import com.example.ex2_phone.ui.components.TransportCard
import com.example.ex2_phone.ui.components.openNotificationSettings
import com.example.ex2_phone.ui.theme.Ink
import com.example.ex2_phone.ui.theme.StrokeSoft
import com.example.ex2_phone.ui.theme.TextHi
import com.example.ex2_phone.ui.theme.TextMid

@Composable
fun SettingsScreen(
    viewModel: CarViewModel,
    carColor: CarColor,
    ipAddress: String,
    onIpChange: (String) -> Unit,
    onConnect: () -> Unit,
    onCarColorChange: (CarColor) -> Unit,
) {
    val isConnected by viewModel.connected.collectAsState()
    val transportMode by viewModel.transportMode.collectAsState()
    val btDevice by viewModel.btDevice.collectAsState()
    val activeTransportLabel by viewModel.activeTransportLabel.collectAsState()
    val bluetoothActive by viewModel.bluetoothActive.collectAsState()
    val autoBackground by viewModel.autoBackground.collectAsState()
    val accent by animateColorAsState(carColor.accent, tween(450), label = "accent")
    val bgTop = lerp(Ink, carColor.accent, 0.08f)

    // Trạng thái thông báo đọc lại mỗi lần màn hình sáng trở lại — user có thể vừa bật/tắt
    // bên Cài đặt hệ thống rồi quay về, không có sự kiện nào báo cho app biết.
    var notificationsEnabled by remember { mutableStateOf(!viewModel.notificationsBlocked()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsEnabled = !viewModel.notificationsBlocked()
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { notificationsEnabled = !viewModel.notificationsBlocked() }

    // Lần đầu vào Cài đặt: xin quyền luôn, khỏi để user mò. Từ chối thì thôi, mục "Thông báo"
    // bên dưới vẫn bấm được bất cứ lúc nào.
    LaunchedEffect(Unit) {
        if (viewModel.shouldAutoAskNotification()) {
            viewModel.markNotificationAsked()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Chạm email → mở thẳng app mail với sẵn người nhận + tiêu đề
    val context = LocalContext.current
    fun openSupportEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$SUPPORT_EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, "Góp ý ứng dụng Geely EX2")
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Không tìm thấy ứng dụng email", Toast.LENGTH_SHORT).show()
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, Ink)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 28.dp),
    ) {
        Text(text = "Cài đặt", color = TextHi, fontSize = 28.sp,
            fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)

        Spacer(Modifier.height(20.dp))

        // --- Kết nối ---
        SectionHeader(icon = Icons.Rounded.Link, title = "Kết nối", accent = accent)
        Spacer(Modifier.height(12.dp))
        TransportCard(
            mode = transportMode,
            onModeChange = viewModel::setTransportMode,
            btDevice = btDevice,
            onBtDeviceChange = viewModel::setBtDevice,
            loadBondedDevices = viewModel::bondedDevices,
            isBluetoothEnabled = viewModel::bluetoothEnabled,
            areNotificationsBlocked = viewModel::notificationsBlocked,
            hasBtPermission = viewModel.hasBluetoothConnect(),
            isConnected = isConnected,
            activeTransportLabel = activeTransportLabel,
            isBluetoothActive = bluetoothActive,
            autoBackground = autoBackground,
            onAutoBackgroundChange = viewModel::setAutoBackground,
            accent = accent,
            onAccent = carColor.onAccent,
            modifier = Modifier.fillMaxWidth(),
        )

        // Ô nhập IP chỉ có nghĩa ở chế độ WiFi.
        if (transportMode == TransportMode.WIFI) {
            Spacer(Modifier.height(12.dp))
            ConnectCard(
                ipAddress = ipAddress,
                onIpChange = onIpChange,
                onConnect = onConnect,
                onDetectIp = { viewModel.detectGatewayIp() },
                isConnected = isConnected,
                accent = accent,
                onAccent = carColor.onAccent,
                modifier = Modifier.fillMaxWidth(),
                showStatus = false,
            )
        }

        Spacer(Modifier.height(24.dp))

        // --- Thông báo ---
        SectionHeader(icon = Icons.Rounded.Notifications, title = "Thông báo", accent = accent)
        Spacer(Modifier.height(12.dp))
        NotificationStatusCard(
            enabled = notificationsEnabled,
            accent = accent,
            onClick = {
                if (canRequestNotificationPermission(context, viewModel)) {
                    viewModel.markNotificationAsked()
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Hết cửa xin: đã từ chối hẳn, quyền đã có mà user tắt ở cấp app/kênh,
                    // hoặc máy chạy dưới API 33. Chỉ còn đường vào Cài đặt hệ thống.
                    context.openNotificationSettings()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))

        // --- Màu xe ---
        SectionHeader(icon = Icons.Rounded.Palette, title = "Màu xe", accent = accent)
        Spacer(Modifier.height(4.dp))
        Text(text = "Chọn màu để đổi màu chủ đạo giao diện", color = TextMid, fontSize = 13.sp,
            modifier = Modifier.padding(start = 40.dp))
        Spacer(Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = accent.copy(alpha = 0.15f)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                CarColor.entries.chunked(3).forEach { rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowColors.forEach { color ->
                            ColorSwatch(
                                carColor = color,
                                isSelected = color == carColor,
                                onClick = { onCarColorChange(color) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - rowColors.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }



        Spacer(Modifier.height(24.dp))

        // --- Giới thiệu ---
        SectionHeader(icon = Icons.Rounded.Info, title = "Giới thiệu", accent = accent)
        Spacer(Modifier.height(12.dp))
        GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = accent.copy(alpha = 0.1f)) {
            Column {
                Text(text = "Geely EX2 Remote", color = TextHi, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(text = "Phiên bản 1.0.0", color = TextMid, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Ứng dụng điều khiển từ xa cho xe Geely EX2.",
                    color = TextMid, fontSize = 13.sp, lineHeight = 20.sp,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Liên hệ: ", color = TextMid, fontSize = 13.sp, lineHeight = 20.sp)
                    Text(
                        text = SUPPORT_EMAIL,
                        color = accent,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { openSupportEmail() },
                    )
                }
            }
        }
    }
}

/**
 * Dialog xin `POST_NOTIFICATIONS` có thật sự hiện ra được không.
 *
 * Android 13+ **im lặng** từ chối sau 2 lần người dùng bấm "Không cho phép": `launch()` trả kết
 * quả denied ngay mà không vẽ gì lên màn hình. Không đoán trước được điều đó thì chạm vào thẻ
 * "Chưa bật thông báo" sẽ như không có chuyện gì xảy ra.
 *
 * `shouldShowRequestPermissionRationale` trả `false` cho cả hai trường hợp "chưa hỏi bao giờ" và
 * "đã từ chối hẳn", nên phải kèm cờ [CarViewModel.notificationAsked] mới phân biệt được.
 */
private fun canRequestNotificationPermission(context: Context, viewModel: CarViewModel): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    if (viewModel.hasNotificationPermission()) return false
    if (!viewModel.notificationAsked()) return true
    val activity = context as? Activity ?: return false
    return activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
}

/**
 * Cho biết thông báo của app đang bật hay tắt, chạm để sửa.
 *
 * "Bật" ở đây là *thấy được thông báo* — cả quyền runtime lẫn công tắc cấp app/kênh trong Cài đặt
 * hệ thống, vì tắt cái nào thì notification chạy nền cũng không hiện.
 */
@Composable
private fun NotificationStatusCard(
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.clickable(onClick = onClick),
        borderColor = if (enabled) accent.copy(alpha = 0.35f) else StrokeSoft,
        glowColor = if (enabled) accent else null,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (enabled) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff,
                contentDescription = null,
                tint = if (enabled) accent else TextMid,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (enabled) "Đã bật thông báo" else "Chưa bật thông báo",
                    color = TextHi,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (enabled) {
                        "Hiện trạng thái kết nối và mức pin khi đóng ứng dụng"
                    } else {
                        "Chạm để bật — không có thông báo thì đóng ứng dụng là mất luôn trạng thái"
                    },
                    color = TextMid,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (enabled) Icons.Rounded.CheckCircle else Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = if (enabled) accent else TextMid,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Email hỗ trợ hiện ở mục Giới thiệu. */
private const val SUPPORT_EMAIL = "thang.dangvan7798@gmail.com"


