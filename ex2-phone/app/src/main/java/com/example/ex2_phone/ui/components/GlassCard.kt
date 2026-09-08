package com.example.ex2_phone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ex2_phone.ui.theme.StrokeSoft
import com.example.ex2_phone.ui.theme.Surface1
import com.example.ex2_phone.ui.theme.Surface2

/**
 * Card phong cách glassmorphism trên nền tối: gradient dọc nhẹ + viền mảnh + vệt sáng
 * trên đỉnh tạo cảm giác kính. Truyền [glowColor] để có quầng sáng theo màu nhấn (dùng
 * cho card đang bật/active).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = StrokeSoft,
    glowColor: Color? = null,
    cornerRadius: Dp = 24.dp,
    contentPadding: Dp = 18.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val fill = if (glowColor != null) {
        Brush.verticalGradient(
            listOf(
                glowColor.copy(alpha = 0.14f),
                Surface2,
                Surface1,
            )
        )
    } else {
        Brush.verticalGradient(listOf(Surface2, Surface1))
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, borderColor, shape)
            .padding(contentPadding),
        content = content
    )
}
