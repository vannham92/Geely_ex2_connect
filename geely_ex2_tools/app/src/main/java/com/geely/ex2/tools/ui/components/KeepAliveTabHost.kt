package com.geely.ex2.tools.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.zIndex
import com.geely.ex2.tools.navigation.AppRoutes

/**
 * Whether the current destination is the selected tab.
 * Hidden kept-alive tabs stay composed but report false so polling can pause.
 */
val LocalTabVisible = compositionLocalOf { true }

private val VisitedRoutesSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)

/**
 * Keeps visited destinations in composition so switching tabs does not rebuild UI.
 * Inactive tabs stay composed but are not placed (no draw, no hit-testing).
 */
@Composable
fun KeepAliveTabHost(
    selectedRoute: String,
    modifier: Modifier = Modifier,
    content: @Composable (route: String) -> Unit,
) {
    val visitedRoutes = rememberSaveable(saver = VisitedRoutesSaver) {
        mutableStateListOf()
    }

    SideEffect {
        if (selectedRoute != AppRoutes.NONE && selectedRoute !in visitedRoutes) {
            visitedRoutes.add(selectedRoute)
        }
    }

    // Include the current route on the first frame before SideEffect persists it.
    val routesToShow = remember(selectedRoute, visitedRoutes.size) {
        buildList {
            addAll(visitedRoutes)
            if (selectedRoute != AppRoutes.NONE && selectedRoute !in visitedRoutes) {
                add(selectedRoute)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (selectedRoute == AppRoutes.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
            ) {
                CompositionLocalProvider(LocalTabVisible provides true) {
                    content(AppRoutes.NONE)
                }
            }
        }

        routesToShow.forEach { route ->
            key(route) {
                val isSelected = route == selectedRoute
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isSelected) 1f else 0f)
                        .keepAliveVisibility(isSelected),
                ) {
                    CompositionLocalProvider(LocalTabVisible provides isSelected) {
                        content(route)
                    }
                }
            }
        }
    }
}

/**
 * Measures children always; places them only when [visible] so kept-alive
 * hidden tabs do not draw or receive pointer input.
 */
private fun Modifier.keepAliveVisibility(visible: Boolean): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            if (visible) {
                placeable.place(0, 0)
            }
        }
    }
