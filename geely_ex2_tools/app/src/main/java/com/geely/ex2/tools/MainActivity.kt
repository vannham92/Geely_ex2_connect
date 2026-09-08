package com.geely.ex2.tools

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.geely.ex2.tools.feature.ambient.ui.AmbientLightScreen
import com.geely.ex2.tools.feature.avas.ui.AvasScreen
import com.geely.ex2.tools.feature.battery.ui.BatteryScreen
import com.geely.ex2.tools.feature.driving.ui.DrivingScreen
import com.geely.ex2.tools.feature.home.ui.EmptyStartScreen
import com.geely.ex2.tools.feature.settings.ui.SettingsScreen
import com.geely.ex2.tools.feature.speed.ui.SpeedScreen
import com.geely.ex2.tools.feature.temperature.ui.TemperatureScreen
import com.geely.ex2.tools.feature.wifi.ui.WifiScreen
import com.geely.ex2.tools.feature.window.ui.AutoWindowScreen
import com.geely.ex2.tools.navigation.AppRoutes
import com.geely.ex2.tools.ui.components.FlymeAppShell
import com.geely.ex2.tools.ui.components.KeepAliveTabHost
import com.geely.ex2.tools.ui.theme.GeelyEx2Background
import com.geely.ex2.tools.ui.theme.GeelyEx2ToolsTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeelyEx2ToolsTheme {
                val view = LocalView.current
                SideEffect {
                    view.isSoundEffectsEnabled = true
                }

                var selectedRoute by rememberSaveable { mutableStateOf(AppRoutes.NONE) }
                val clearSelection = { selectedRoute = AppRoutes.NONE }

                GeelyEx2Background(modifier = Modifier.fillMaxSize()) {
                    FlymeAppShell(
                        currentRoute = selectedRoute,
                        onDestinationSelected = { route ->
                            if (route != selectedRoute) {
                                selectedRoute = route
                            }
                        },
                        onBack = clearSelection,
                    ) {
                        KeepAliveTabHost(selectedRoute = selectedRoute) { route ->
                            when (route) {
                                AppRoutes.NONE -> EmptyStartScreen()
                                AppRoutes.WIFI -> WifiScreen(onBack = clearSelection)
                                AppRoutes.TEMPERATURE -> TemperatureScreen(onBack = clearSelection)
                                AppRoutes.SPEED -> SpeedScreen(onBack = clearSelection)
                                AppRoutes.BATTERY -> BatteryScreen(onBack = clearSelection)
                                AppRoutes.DRIVING -> DrivingScreen(onBack = clearSelection)
                                AppRoutes.AMBIENT_LIGHT -> AmbientLightScreen(onBack = clearSelection)
                                AppRoutes.AVAS -> AvasScreen(onBack = clearSelection)
                                AppRoutes.AUTO_WINDOW -> AutoWindowScreen(onBack = clearSelection)
                                AppRoutes.SETTINGS -> SettingsScreen(onBack = clearSelection)
                                else -> EmptyStartScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}
