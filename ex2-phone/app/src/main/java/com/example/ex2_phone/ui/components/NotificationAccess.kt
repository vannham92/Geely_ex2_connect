package com.example.ex2_phone.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ex2_phone.ui.theme.Surface1
import com.example.ex2_phone.ui.theme.TextHi
import com.example.ex2_phone.ui.theme.TextMid

/**
 * Dùng chung cho mọi chỗ cần thông báo bật: mục "Thông báo" trong Cài đặt và công tắc chạy nền.
 */

/** Mở thẳng trang thông báo của app; máy nào không có thì rơi về trang thông tin ứng dụng. */
internal fun Context.openNotificationSettings() {
    val appNotifications = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    try {
        startActivity(appNotifications)
    } catch (e: ActivityNotFoundException) {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", packageName, null)),
            )
        } catch (e2: ActivityNotFoundException) {
            Toast.makeText(this, "Không mở được cài đặt thông báo", Toast.LENGTH_SHORT).show()
        }
    }
}

/** Nhắc bật thông báo — chạy nền mà tắt thông báo thì không còn chỗ nào thấy trạng thái. */
@Composable
internal fun NotificationPromptDialog(
    accent: Color,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        titleContentColor = TextHi,
        textContentColor = TextMid,
        shape = RoundedCornerShape(24.dp),
        title = { Text(text = "Bật thông báo", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "Chạy nền cần thông báo để hiện trạng thái kết nối và mức pin khi bạn " +
                    "đóng ứng dụng. Thông báo đang bị tắt — bật lại trong Cài đặt nhé.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Mở cài đặt", color = accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Để sau", color = TextMid) }
        },
    )
}
