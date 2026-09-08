package com.example.ex2_phone.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Chủ đề tối cố định của app. KHÔNG dùng dynamic color (màu nền máy) — thương hiệu
 * và màu nhấn theo màu sơn xe phải luôn thắng. Màu nhấn được tô từ [CarColor] tại chỗ.
 */
private val CarDarkScheme = darkColorScheme(
    primary = BatteryGreen,
    onPrimary = Color.Black,
    secondary = RangeBlue,
    tertiary = OdoAmber,
    background = Ink,
    onBackground = TextHi,
    surface = Surface1,
    onSurface = TextHi,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMid,
    outline = StrokeMid,
    error = Danger,
    onError = Color.Black,
)

@Composable
fun Ex2phoneTheme(
    darkTheme: Boolean = true,
    // Giữ tham số cho tương thích, nhưng luôn dùng bảng màu tối riêng của app.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Thanh trạng thái / điều hướng trong suốt, icon sáng cho nền tối.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = CarDarkScheme,
        typography = Typography,
        content = content
    )
}
