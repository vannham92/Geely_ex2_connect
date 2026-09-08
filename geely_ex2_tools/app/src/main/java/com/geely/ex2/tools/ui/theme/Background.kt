package com.geely.ex2.tools.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance

@Composable
fun GeelyEx2Background(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val gradientColors = if (isDarkTheme) {
        listOf(FlymeBackgroundDarkTop, FlymeBackgroundDarkBottom)
    } else {
        listOf(FlymeBackgroundLightTop, FlymeBackgroundLightBottom)
    }
    val glow = if (isDarkTheme) FlymeGlowDark else FlymeGlowLight

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(colors = gradientColors),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glow, glow.copy(alpha = 0f)),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.15f, size.height * 0.08f),
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.15f, size.height * 0.08f),
            )
        }
        content()
    }
}
