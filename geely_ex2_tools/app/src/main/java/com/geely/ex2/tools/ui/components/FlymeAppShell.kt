package com.geely.ex2.tools.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

private const val CompactRailMaxWidthDp = 600

@Composable
fun FlymeAppShell(
    currentRoute: String?,
    onDestinationSelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < CompactRailMaxWidthDp
    // Home hidden — no rail Back that redirects to Home.
    val showBack = false
    // val showBack = currentRoute != null && currentRoute != AppRoutes.HOME

    Row(modifier = modifier.fillMaxSize()) {
        FlymeVerticalNav(
            currentRoute = currentRoute,
            onDestinationSelected = onDestinationSelected,
            onBack = onBack,
            showBack = showBack,
            compact = isCompact,
            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars),
        )
        VerticalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        )
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            content()
        }
    }
}

@Composable
fun isFlymeRailCompact(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp < CompactRailMaxWidthDp
}
