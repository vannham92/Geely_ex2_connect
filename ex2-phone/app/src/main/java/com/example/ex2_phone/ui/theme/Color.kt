package com.example.ex2_phone.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Bảng màu giao diện — phong cách xe điện hiện đại (Tesla / Polestar): nền gần như
 * đen ngả xanh, chữ trắng ngà, một màu nhấn nổi bật lấy từ màu sơn xe.
 *
 * Màu nhấn (accent) KHÔNG nằm ở đây — nó đến từ [com.example.ex2_phone.ui.CarColor].
 * File này chỉ giữ các màu nền / bề mặt / chữ / màu ngữ nghĩa dùng chung.
 */

// --- Nền & bề mặt ---
val Ink = Color(0xFF090B10)        // nền sâu nhất của toàn app
val InkElevated = Color(0xFF0F1218) // nền vùng phía trên (gradient hero)
val Surface1 = Color(0xFF141821)    // nền card thường
val Surface2 = Color(0xFF1B2029)    // nền card nổi hơn / control
val SurfaceHi = Color(0xFF232A35)   // ô control ở trạng thái nghỉ

// --- Viền / đường kẻ ---
val StrokeSoft = Color(0x14FFFFFF)  // ~8% trắng
val StrokeMid = Color(0x1FFFFFFF)   // ~12% trắng

// --- Chữ ---
val TextHi = Color(0xFFF4F6FA)      // tiêu đề, số liệu
val TextMid = Color(0xFF9AA3B2)     // phụ đề, nhãn
val TextLow = Color(0xFF5F6875)     // chữ mờ, gợi ý

// --- Màu ngữ nghĩa (số liệu / trạng thái) ---
val BatteryGreen = Color(0xFF29D07E) // pin
val RangeBlue = Color(0xFF4EA1FF)    // quãng đường còn lại
val OdoAmber = Color(0xFFFFB020)     // quãng đường đã đi
val Danger = Color(0xFFFF5A5A)       // lỗi / mất kết nối
val Warn = Color(0xFFFFB020)         // đang kết nối

// --- Màu chế độ nhanh điều hoà ---
val QuickCoolBlue = Color(0xFF4E9BFF)  // làm mát nhanh
val QuickWarmRust = Color(0xFFB4553C)  // làm ấm nhanh (nâu đỏ)

// --- Màu theo chế độ lái ---
val EcoGreen = Color(0xFF7BD88F)
val ComfortBlue = Color(0xFF6FB4FF)
val SportOrange = Color(0xFFFF8A5B)
