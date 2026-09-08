package com.geely.ex2.tools.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geely.ex2.tools.R
import com.geely.ex2.tools.data.app.openDeveloperTelegram
import com.geely.ex2.tools.ui.clickableWithSystemSound
import com.geely.ex2.tools.ui.rememberOnClickWithSystemSound
import com.geely.ex2.tools.ui.theme.GeelyEx2ToolsTheme

@Composable
fun DeveloperAboutDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val onDismissWithSound = rememberOnClickWithSystemSound(onDismiss)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        confirmButton = {
            TextButton(onClick = onDismissWithSound) {
                Text(
                    text = stringResource(R.string.home_about_close),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.home_about_developer),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.developer_about_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.home_about_telegram),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickableWithSystemSound {
                        openDeveloperTelegram(context)
                    },
                )
            }
        },
    )
}

@Preview(showBackground = true, name = "Developer About Light")
@Composable
private fun DeveloperAboutDialogPreviewLight() {
    GeelyEx2ToolsTheme(darkTheme = false) {
        DeveloperAboutDialog(onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Developer About Dark")
@Composable
private fun DeveloperAboutDialogPreviewDark() {
    GeelyEx2ToolsTheme(darkTheme = true) {
        DeveloperAboutDialog(onDismiss = {})
    }
}
