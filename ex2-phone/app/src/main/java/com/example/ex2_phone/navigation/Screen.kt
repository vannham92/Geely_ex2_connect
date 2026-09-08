package com.example.ex2_phone.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Home : Screen("home", "Tổng quan", Icons.Filled.DirectionsCar, Icons.Outlined.DirectionsCar)
    data object Driving : Screen("driving", "Lái xe", Icons.Filled.Speed, Icons.Outlined.Speed)
    data object Control : Screen("control", "Điều khiển", Icons.Filled.Tune, Icons.Outlined.Tune)
    data object Hvac : Screen("hvac", "Điều hoà", Icons.Filled.DeviceThermostat, Icons.Outlined.DeviceThermostat)
    data object Settings : Screen("settings", "Cài đặt", Icons.Filled.Settings, Icons.Outlined.Settings)

    companion object {
        val items = listOf(Home, Driving, Control, Hvac, Settings)
    }
}
