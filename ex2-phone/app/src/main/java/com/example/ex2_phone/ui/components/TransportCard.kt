package com.example.ex2_phone.ui.components

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.ex2_phone.data.network.BondedDevice
import com.example.ex2_phone.data.network.TransportMode
import com.example.ex2_phone.ui.theme.Ink
import com.example.ex2_phone.ui.theme.StrokeMid
import com.example.ex2_phone.ui.theme.Surface1
import com.example.ex2_phone.ui.theme.SurfaceHi
import com.example.ex2_phone.ui.theme.TextHi
import com.example.ex2_phone.ui.theme.TextLow
import com.example.ex2_phone.ui.theme.TextMid

private val MODES = listOf(TransportMode.BLUETOOTH, TransportMode.WIFI)

/**
 * Card chọn ống dẫn tới xe — Bluetooth hoặc WiFi, phải chọn hẳn một cái (không có chế độ tự dò),
 * kèm thiết bị kết nối và công tắc chạy nền.
 *
 * Danh sách head unit nằm trong modal thay vì đổ thẳng ra card — máy nào cũng pair sẵn cả tá
 * thiết bị (tai nghe, loa…), liệt kê hết thì cả màn Cài đặt bị nuốt chửng. Ngoài card chỉ hiện
 * đúng thiết bị đang chọn.
 *
 * Bluetooth chỉ cần chọn một lần; sau đó nối tự động mỗi khi lên xe (bond đã có sẵn từ pairing
 * handsfree nên hệ điều hành không hỏi gì thêm).
 */
@Composable
fun TransportCard(
    mode: TransportMode,
    onModeChange: (TransportMode) -> Unit,
    btDevice: BondedDevice?,
    onBtDeviceChange: (BondedDevice?) -> Unit,
    loadBondedDevices: () -> List<BondedDevice>,
    isBluetoothEnabled: () -> Boolean,
    areNotificationsBlocked: () -> Boolean,
    hasBtPermission: Boolean,
    isConnected: Boolean,
    activeTransportLabel: String?,
    isBluetoothActive: Boolean,
    autoBackground: Boolean,
    onAutoBackgroundChange: (Boolean) -> Unit,
    accent: Color,
    onAccent: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasBtPermission) }
    var devices by remember { mutableStateOf(emptyList<BondedDevice>()) }
    var showPicker by remember { mutableStateOf(false) }
    var showNotificationPrompt by remember { mutableStateOf(false) }
    var btEnabled by remember { mutableStateOf(isBluetoothEnabled()) }

    // Quay lại app từ Cài đặt hệ thống → đọc lại.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { btEnabled = isBluetoothEnabled() }

    // Gạt Bluetooth từ thanh cài đặt nhanh **trong lúc app đang mở**: kéo shade xuống KHÔNG làm
    // Activity pause nên ON_RESUME không bắn, state sẽ đứng hình. Nghe broadcast để bắt kịp.
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                btEnabled = isBluetoothEnabled()
                if (!btEnabled) showPicker = false   // danh sách vừa rỗng, để mở là vô nghĩa
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { ok -> granted = ok }

    // Hệ thống hỏi "Cho phép bật Bluetooth?"; đồng ý thì mở luôn danh sách thiết bị.
    val enableBluetooth = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        btEnabled = isBluetoothEnabled()   // tin adapter, không tin resultCode
        if (btEnabled) {
            devices = loadBondedDevices()
            showPicker = true
        }
    }

    // Service chạy nền phải có notification thường trực; API 33+ đòi quyền mới hiện được.
    // Từ chối (hoặc đã từ chối 2 lần nên hệ thống không hỏi nữa) → mời vào Cài đặt bật tay.
    // Dù sao service vẫn chạy, chỉ là không nhìn thấy trạng thái.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { ok -> if (!ok) showNotificationPrompt = true }

    /** Bật chạy nền mà thông báo đang tắt → xin quyền, hết đường xin thì mời vào Cài đặt. */
    fun ensureNotificationsEnabled() {
        if (!areNotificationsBlocked()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Trước API 33 không có quyền runtime — tắt thông báo chỉ sửa được trong Cài đặt.
            showNotificationPrompt = true
        }
    }

    // Nạp lại danh sách khi vừa được cấp quyền, vừa bật Bluetooth, hoặc đổi sang chế độ Bluetooth.
    LaunchedEffect(granted, btEnabled, mode) {
        devices = if (granted && btEnabled && mode == TransportMode.BLUETOOTH) {
            loadBondedDevices()
        } else {
            emptyList()
        }
    }

    GlassCard(modifier = modifier, borderColor = accent.copy(alpha = 0.15f)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SegmentedControl(
                options = MODES.map { SegOption(it.label(), it.icon()) },
                selectedIndex = MODES.indexOf(mode).coerceAtLeast(0),
                onSelect = { onModeChange(MODES[it]) },
                accent = accent,
                onAccent = onAccent,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
            Text(text = mode.hint(), color = TextMid, fontSize = 13.sp, lineHeight = 18.sp)

            if (mode == TransportMode.BLUETOOTH) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Thiết bị kết nối",
                    color = TextHi,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))

                if (!granted) {
                    Button(
                        onClick = { permLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                    ) {
                        Text("Cấp quyền Bluetooth", color = onAccent, fontWeight = FontWeight.Bold)
                    }
                } else {
                    SelectedDeviceRow(
                        device = btDevice,
                        bluetoothEnabled = btEnabled,
                        accent = accent,
                        onClick = {
                            // Đọc lại adapter ngay lúc chạm — chốt chặn cuối nếu state kịp cũ đi.
                            btEnabled = isBluetoothEnabled()
                            if (!btEnabled) {
                                enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                            } else {
                                devices = loadBondedDevices()   // pair thêm ngoài app vẫn thấy ngay
                                showPicker = true
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isConnected) Icons.Rounded.CheckCircle else Icons.Rounded.Sync,
                    contentDescription = null,
                    tint = if (isConnected) accent else TextMid,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isConnected && activeTransportLabel != null) {
                        "Đang dùng $activeTransportLabel"
                    } else {
                        "Chưa kết nối — app tự thử lại ngầm"
                    },
                    color = if (isConnected) TextHi else TextMid,
                    fontSize = 13.sp,
                )
            }

            // Chỉ hiện khi đang thật sự chạy trên Bluetooth: ACL_CONNECTED/DISCONNECTED của head
            // unit là thứ bật/tắt service, nên chạy nền vô nghĩa với WiFi.
            if (isBluetoothActive && btDevice != null) {
                Spacer(Modifier.height(12.dp))
                AutoBackgroundRow(
                    checked = autoBackground,
                    onCheckedChange = { enabled ->
                        onAutoBackgroundChange(enabled)
                        if (enabled) ensureNotificationsEnabled()
                    },
                    accent = accent,
                )
                if (autoBackground) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Kết nối nền hay đứt? Tắt tối ưu pin cho ứng dụng",
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            try {
                                context.startActivity(
                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                                )
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "Máy không có mục tối ưu pin", Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                }
            }
        }
    }

    if (showNotificationPrompt) {
        NotificationPromptDialog(
            accent = accent,
            onOpenSettings = {
                showNotificationPrompt = false
                context.openNotificationSettings()
            },
            onDismiss = { showNotificationPrompt = false },
        )
    }

    if (showPicker) {
        DevicePickerDialog(
            devices = devices,
            selected = btDevice,
            accent = accent,
            onSelect = { device ->
                onBtDeviceChange(device)
                showPicker = false
                // Chọn xong head unit là chạy nền có hiệu lực từ lần ACL_CONNECTED kế tiếp,
                // nên lo quyền thông báo ngay tại đây — công tắc chạy nền chỉ hiện sau khi
                // đã nối được Bluetooth, đợi tới đó thì user mất luôn notification lần đầu.
                if (device != null && autoBackground) ensureNotificationsEnabled()
            },
            onReload = { devices = loadBondedDevices() },
            onDismiss = { showPicker = false },
        )
    }
}

/**
 * Ô hiện thiết bị đang chọn (tên + địa chỉ MAC); bấm vào để mở modal đổi thiết bị.
 * Bluetooth đang tắt thì ô này thành nút bật Bluetooth.
 */
@Composable
private fun SelectedDeviceRow(
    device: BondedDevice?,
    bluetoothEnabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceHi.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (bluetoothEnabled) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
            contentDescription = null,
            tint = if (device != null && bluetoothEnabled) accent else StrokeMid,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    !bluetoothEnabled -> "Bluetooth đang tắt"
                    device != null -> device.name
                    else -> "Chưa chọn thiết bị"
                },
                color = if (device != null && bluetoothEnabled) TextHi else TextMid,
                fontSize = 14.sp,
                fontWeight = if (device != null && bluetoothEnabled) FontWeight.Bold else FontWeight.Normal,
            )
            Text(
                text = when {
                    !bluetoothEnabled -> "Chạm để bật Bluetooth"
                    device != null -> device.address
                    else -> "Chạm để chọn head unit của xe"
                },
                color = TextMid,
                fontSize = 11.sp,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = TextMid,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Công tắc chạy nền, đặt ngay dưới dòng trạng thái kết nối cho gọn. */
@Composable
private fun AutoBackgroundRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Autorenew,
            contentDescription = null,
            tint = if (checked) accent else TextMid,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Chạy nền tự động", color = TextHi, fontSize = 13.sp)
            Text(
                text = if (checked) {
                    "Tự kết nối khi xe bắt Bluetooth"
                } else {
                    "Chỉ kết nối khi mở ứng dụng"
                },
                color = if (checked) accent else TextLow,
                fontSize = 11.sp,
            )
        }
        // Màu nhấn sáng (Trắng, Be…) làm núm trắng biến mất vào rãnh → đổi núm sang màu tối.
        val lightAccent = accent.luminance() > 0.6f
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (lightAccent) Ink else Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = TextMid,
                uncheckedTrackColor = SurfaceHi,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

/** Modal chọn head unit. Chạm lại thiết bị đang chọn = bỏ chọn. */
@Composable
private fun DevicePickerDialog(
    devices: List<BondedDevice>,
    selected: BondedDevice?,
    accent: Color,
    onSelect: (BondedDevice?) -> Unit,
    onReload: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        titleContentColor = TextHi,
        textContentColor = TextMid,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(text = "Chọn thiết bị kết nối", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            if (devices.isEmpty()) {
                Text(
                    text = "Chưa có thiết bị nào đã ghép đôi. Hãy pair điện thoại với xe trước.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    devices.forEach { device ->
                        DeviceRow(
                            device = device,
                            selected = device.address == selected?.address,
                            accent = accent,
                            onClick = {
                                onSelect(if (device.address == selected?.address) null else device)
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onReload) {
                Text("Tải lại", color = TextMid)
            }
        },
    )
}

@Composable
private fun DeviceRow(
    device: BondedDevice,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Bluetooth,
                contentDescription = null,
                tint = if (selected) accent else StrokeMid,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    color = if (selected) accent else TextHi,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
                Text(text = device.address, color = TextMid, fontSize = 11.sp)
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private fun TransportMode.label() = when (this) {
    TransportMode.BLUETOOTH -> "Bluetooth"
    TransportMode.WIFI -> "WiFi"
}

private fun TransportMode.icon() = when (this) {
    TransportMode.BLUETOOTH -> Icons.Rounded.Bluetooth
    TransportMode.WIFI -> Icons.Rounded.Wifi
}

private fun TransportMode.hint() = when (this) {
    TransportMode.BLUETOOTH -> "Nối qua Bluetooth. Không cần bật WiFi trên xe."
    TransportMode.WIFI -> "Nối qua WiFi. Điện thoại và xe phải cùng mạng."
}
