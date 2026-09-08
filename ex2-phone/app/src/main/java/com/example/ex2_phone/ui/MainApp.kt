package com.example.ex2_phone.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ex2_phone.R
import com.example.ex2_phone.navigation.Screen

private data class TabItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/**
 * Tab "Tổng quan" dùng icon xe EX2 riêng (`ic_car_ex2`) nên danh sách phải dựng trong composition —
 * `vectorResource` là `@Composable`, không đặt được ở top-level `val`.
 */
@Composable
private fun rememberTabs(): List<TabItem> {
    val carIcon = ImageVector.vectorResource(R.drawable.ic_car_ex2)
    return remember(carIcon) {
        listOf(
            TabItem(Screen.Home, carIcon, carIcon),
            TabItem(Screen.Driving, Icons.Filled.Speed, Icons.Outlined.Speed),
            TabItem(Screen.Control, Icons.Filled.Tune, Icons.Outlined.Tune),
            TabItem(Screen.Hvac, Icons.Filled.DeviceThermostat, Icons.Outlined.DeviceThermostat),
            TabItem(Screen.Settings, Icons.Filled.Settings, Icons.Outlined.Settings),
        )
    }
}

@Composable
fun MainApp(carViewModel: CarViewModel = viewModel()) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    var carColor by rememberSaveable {
        mutableStateOf(
            sharedPreferences.getString("car_color", null)
                ?.let { name -> runCatching { CarColor.valueOf(name) }.getOrNull() }
                ?: CarColor.DEFAULT
        )
    }
    var ipAddress by remember { mutableStateOf(sharedPreferences.getString("ip_address", "10.77.86.7") ?: "10.77.86.7") }

    val accent by animateColorAsState(carColor.accent, tween(450), label = "nav_accent")

    // Dùng chung cho card kết nối ở cả Tổng quan và Cài đặt.
    val onConnect = {
        sharedPreferences.edit().putString("ip_address", ipAddress).apply()
        carViewModel.connect(ipAddress)
    }

    // Chuyển tab (thanh dưới và các mục bấm được ở Tổng quan dùng chung).
    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        carViewModel.connect(ipAddress)
    }

    // Lệnh thất bại → hiện Snackbar (server-authoritative đã tự rollback status).
    LaunchedEffect(Unit) {
        carViewModel.commandError.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0C0F14),
                tonalElevation = 0.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                rememberTabs().forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == tab.screen.route
                    } == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.screen.label,
                            )
                        },
                        label = {
                            Text(
                                text = tab.screen.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                            )
                        },
                        selected = selected,
                        onClick = { navigateToTab(tab.screen.route) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accent,
                            selectedTextColor = accent,
                            unselectedIconColor = Color(0xFF5F6875),
                            unselectedTextColor = Color(0xFF5F6875),
                            indicatorColor = accent.copy(alpha = 0.16f),
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = carViewModel,
                    carColor = carColor,
                    ipAddress = ipAddress,
                    onIpChange = { ipAddress = it },
                    onConnect = onConnect,
                    onOpenSettings = { navigateToTab(Screen.Settings.route) },
                    onOpenHvac = { navigateToTab(Screen.Hvac.route) },
                )
            }
            composable(Screen.Driving.route) {
                DrivingScreen(viewModel = carViewModel, carColor = carColor)
            }
            composable(Screen.Control.route) {
                ControlScreen(viewModel = carViewModel, carColor = carColor)
            }
            composable(Screen.Hvac.route) {
                HvacScreen(viewModel = carViewModel, carColor = carColor)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = carViewModel,
                    carColor = carColor,
                    ipAddress = ipAddress,
                    onIpChange = { ipAddress = it },
                    onConnect = onConnect,
                    onCarColorChange = {
                        carColor = it
                        // commit() (không phải apply()) để màu chắc chắn nằm trên đĩa
                        // ngay cả khi người dùng vuốt tắt app ngay sau khi chọn.
                        sharedPreferences.edit().putString("car_color", it.name).commit()
                    },
                )
            }
        }
    }
}
