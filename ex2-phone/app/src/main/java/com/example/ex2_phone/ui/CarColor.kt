package com.example.ex2_phone.ui

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.ex2_phone.R

/**
 * Màu sơn xe. Mỗi màu sinh ra một "màu chủ đạo" (accent) cho toàn bộ giao diện.
 *
 * - [swatch]   : màu hiển thị trong ô chọn, đúng mã màu sơn thật của xe.
 * - [accent]   : màu nhấn dùng cho UI — luôn bằng [swatch], không chỉnh sáng/tối.
 * - [onAccent] : màu chữ/icon nằm trên nền [accent].
 * - [onSwatch] : màu dấu check nằm trên [swatch].
 * - [imageRes] : ảnh xe đúng màu sơn, hiển thị ở màn hình chính.
 */
enum class CarColor(
    val label: String,
    val swatch: Color,
    val accent: Color,
    val onAccent: Color,
    val onSwatch: Color,
    @param:DrawableRes val imageRes: Int,
) {
    WHITE(
        label = "Trắng",
        swatch = Color(0xFFFFFFFF),
        accent = Color(0xFFFFFFFF),
        onAccent = Color.Black,
        onSwatch = Color.Black,
        imageRes = R.drawable.ex2_white,
    ),
    BEIGE(
        label = "Be",
        swatch = Color(0xFFEDEDBF),
        accent = Color(0xFFEDEDBF),
        onAccent = Color.Black,
        onSwatch = Color.Black,
        imageRes = R.drawable.ex2_beige,
    ),
    SILVER(
        label = "Bạc",
        swatch = Color(0xFF919DA5),
        accent = Color(0xFF919DA5),
        onAccent = Color.Black,
        onSwatch = Color.Black,
        imageRes = R.drawable.ex2_silver,
    ),
    DARK_GRAY(
        label = "Xám đen",
        swatch = Color(0xFF666666),
        accent = Color(0xFF666666),
        onAccent = Color.White,
        onSwatch = Color.White,
        imageRes = R.drawable.ex2_gray,
    ),
    AVOCADO_GREEN(
        label = "Xanh bơ",
        swatch = Color(0xFF8ACF8A),
        accent = Color(0xFF8ACF8A),
        onAccent = Color.Black,
        onSwatch = Color.Black,
        imageRes = R.drawable.ex2_green,
    ),
    PINK_PURPLE(
        label = "Hồng tím",
        swatch = Color(0xFF8A727A),
        accent = Color(0xFF8A727A),
        onAccent = Color.White,
        onSwatch = Color.White,
        imageRes = R.drawable.ex2_pink,
    );

    companion object {
        val DEFAULT = BEIGE
    }
}
