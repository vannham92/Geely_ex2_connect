package com.example.ex2_phone.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ex2_phone.ui.theme.StrokeMid
import com.example.ex2_phone.ui.theme.TextHi
import com.example.ex2_phone.ui.theme.TextMid

/**
 * Card kết nối: ô nhập IP xe + nút "Kết nối" + nút dò IP từ hotspot.
 * Dùng ở màn Cài đặt (luôn hiện) và màn Tổng quan (chỉ hiện khi chưa kết nối).
 *
 * [onDetectIp] trả IP gateway của WiFi đang nối, `null` nếu không dò được.
 * [showStatus] bật huy hiệu trạng thái ở cuối card — tắt khi màn hình đã có sẵn huy hiệu.
 */
@Composable
fun ConnectCard(
    ipAddress: String,
    onIpChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDetectIp: () -> String?,
    isConnected: Boolean,
    accent: Color,
    onAccent: Color,
    modifier: Modifier = Modifier,
    description: String = "Nhập địa chỉ IP WiFi trên xe để kết nối điều khiển từ xa",
    showStatus: Boolean = true,
) {
    val context = LocalContext.current

    GlassCard(modifier = modifier, borderColor = accent.copy(alpha = 0.15f)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = description, color = TextMid, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = onIpChange,
                    label = { Text("Địa chỉ IP xe", color = TextMid) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextHi,
                        unfocusedTextColor = TextHi,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = StrokeMid,
                        cursorColor = accent
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Text("Kết nối", color = onAccent, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(
                onClick = {
                    val ip = onDetectIp()
                    if (ip != null) {
                        onIpChange(ip)
                        Toast.makeText(context, "Đã dò IP: $ip", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Không tìm thấy gateway. Hãy nối vào hotspot của xe trước.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Text("Tự động dò IP từ hotspot xe", color = accent, fontSize = 13.sp)
            }
            if (showStatus) {
                Spacer(Modifier.height(8.dp))
                StatusPill(isConnected = isConnected)
            }
        }
    }
}
