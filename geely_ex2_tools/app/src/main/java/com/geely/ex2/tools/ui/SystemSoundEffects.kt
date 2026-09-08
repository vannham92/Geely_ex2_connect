package com.geely.ex2.tools.ui

import android.view.SoundEffectConstants
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role

@Composable
fun rememberSystemClickSound(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        { view.playSoundEffect(SoundEffectConstants.CLICK) }
    }
}

@Composable
fun rememberOnClickWithSystemSound(onClick: () -> Unit): () -> Unit {
    val playClick = rememberSystemClickSound()
    return remember(onClick, playClick) {
        {
            playClick()
            onClick()
        }
    }
}

@Composable
fun rememberOnCheckedChangeWithSystemSound(onCheckedChange: (Boolean) -> Unit): (Boolean) -> Unit {
    val playClick = rememberSystemClickSound()
    return remember(onCheckedChange, playClick) {
        { checked ->
            playClick()
            onCheckedChange(checked)
        }
    }
}

fun Modifier.clickableWithSystemSound(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val playClick = rememberSystemClickSound()
    clickable(
        enabled = enabled,
        role = Role.Button,
        onClick = {
            playClick()
            onClick()
        },
    )
}
