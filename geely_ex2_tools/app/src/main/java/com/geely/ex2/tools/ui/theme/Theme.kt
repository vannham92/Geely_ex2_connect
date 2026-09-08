package com.geely.ex2.tools.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class FlymeExtraColors(
    val rail: Color,
    val switchTrackOff: Color,
)

val LocalFlymeExtraColors = staticCompositionLocalOf {
    FlymeExtraColors(
        rail = FlymeRailLight,
        switchTrackOff = FlymeSwitchTrackOffLight,
    )
}

private val LightColorScheme = lightColorScheme(
    primary = FlymeAccent,
    onPrimary = Color.White,
    secondary = GeelyBlueLight,
    onSecondary = Color.White,
    background = FlymeBackgroundLight,
    surface = FlymeSurfaceLight,
    surfaceVariant = FlymeSurfaceVariantLight,
    onSurface = FlymeOnSurfaceLight,
    onSurfaceVariant = FlymeOnSurfaceVariantLight,
    outlineVariant = FlymeOutlineVariantLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = FlymeAccent,
    onPrimary = Color.White,
    secondary = GeelyBlueLight,
    onSecondary = Color.White,
    background = FlymeBackgroundDark,
    surface = FlymeSurfaceDark,
    surfaceVariant = FlymeSurfaceVariantDark,
    onSurface = FlymeOnSurfaceDark,
    onSurfaceVariant = FlymeOnSurfaceVariantDark,
    outlineVariant = FlymeOutlineVariantDark,
)

private val LightExtraColors = FlymeExtraColors(
    rail = FlymeRailLight,
    switchTrackOff = FlymeSwitchTrackOffLight,
)

private val DarkExtraColors = FlymeExtraColors(
    rail = FlymeRailDark,
    switchTrackOff = FlymeSwitchTrackOffDark,
)

object FlymeTheme {
    val extraColors: FlymeExtraColors
        @Composable
        get() = LocalFlymeExtraColors.current
}

@Composable
fun GeelyEx2ToolsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalFlymeExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
