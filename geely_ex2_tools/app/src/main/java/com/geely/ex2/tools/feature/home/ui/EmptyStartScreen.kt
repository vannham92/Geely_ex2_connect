package com.geely.ex2.tools.feature.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.geely.ex2.tools.ui.theme.GeelyEx2ToolsTheme

@Composable
fun EmptyStartScreen(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize())
}

@Preview(showBackground = true, name = "Empty Start Light")
@Composable
private fun EmptyStartScreenPreviewLight() {
    GeelyEx2ToolsTheme(darkTheme = false) {
        EmptyStartScreen()
    }
}

@Preview(showBackground = true, name = "Empty Start Dark")
@Composable
private fun EmptyStartScreenPreviewDark() {
    GeelyEx2ToolsTheme(darkTheme = true) {
        EmptyStartScreen()
    }
}
