package com.geely.ex2.tools.ui.components

import androidx.compose.foundation.background
import com.geely.ex2.tools.ui.clickableWithSystemSound
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.SensorDoor
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.geely.ex2.tools.R
import com.geely.ex2.tools.navigation.AppRoutes
import com.geely.ex2.tools.ui.theme.FlymeTheme

data class FlymeNavDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

val FlymeNavDestinations = listOf(
    // FlymeNavDestination(
    //     route = AppRoutes.HOME,
    //     labelRes = R.string.nav_home,
    //     icon = Icons.Filled.Home,
    // ),
    FlymeNavDestination(
        route = AppRoutes.WIFI,
        labelRes = R.string.tool_wifi_title,
        icon = Icons.Filled.Wifi,
    ),
    FlymeNavDestination(
        route = AppRoutes.TEMPERATURE,
        labelRes = R.string.tool_temperature_title,
        icon = Icons.Filled.Thermostat,
    ),
    FlymeNavDestination(
        route = AppRoutes.SPEED,
        labelRes = R.string.tool_speed_title,
        icon = Icons.Filled.Speed,
    ),
    FlymeNavDestination(
        route = AppRoutes.DRIVING,
        labelRes = R.string.tool_driving_title,
        icon = Icons.Filled.DirectionsCar,
    ),
    FlymeNavDestination(
        route = AppRoutes.BATTERY,
        labelRes = R.string.tool_battery_title,
        icon = Icons.Filled.BatteryChargingFull,
    ),
    FlymeNavDestination(
        route = AppRoutes.AMBIENT_LIGHT,
        labelRes = R.string.tool_ambient_light_title,
        icon = Icons.Filled.LightMode,
    ),
    FlymeNavDestination(
        route = AppRoutes.AVAS,
        labelRes = R.string.tool_avas_title,
        icon = Icons.AutoMirrored.Filled.VolumeOff,
    ),
    FlymeNavDestination(
        route = AppRoutes.AUTO_WINDOW,
        labelRes = R.string.tool_auto_window_title,
        icon = Icons.Filled.SensorDoor,
    ),
    FlymeNavDestination(
        route = AppRoutes.SETTINGS,
        labelRes = R.string.tool_settings_title,
        icon = Icons.Filled.Settings,
    ),
)

@Composable
fun FlymeVerticalNav(
    currentRoute: String?,
    onDestinationSelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    compact: Boolean = false,
) {
    val railWidth = if (compact) 144.dp else 336.dp

    Column(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .background(FlymeTheme.extraColors.rail)
            .padding(top = 20.dp, bottom = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 16.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FlymeNavDestinations.forEach { destination ->
                val isSelected = currentRoute == destination.route
                FlymeNavItem(
                    label = stringResource(destination.labelRes),
                    icon = destination.icon,
                    selected = isSelected,
                    compact = compact,
                    onClick = { onDestinationSelected(destination.route) },
                )
            }
        }

        if (showBack) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (compact) 16.dp else 36.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickableWithSystemSound(onClick = onBack)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (compact) Arrangement.Center else Arrangement.Start,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.nav_back),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }
                if (!compact) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.nav_back),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlymeNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val background = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickableWithSystemSound(onClick = onClick)
            .padding(horizontal = if (compact) 0.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (compact) Arrangement.Center else Arrangement.Start,
    ) {
        if (!compact && selected) {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        } else if (!compact) {
            Spacer(modifier = Modifier.width(16.dp))
        }

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(32.dp),
        )

        if (!compact) {
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
