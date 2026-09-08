package com.geely.ex2.tools.feature.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geely.ex2.tools.R
import com.geely.ex2.tools.data.app.getAppVersionName
import com.geely.ex2.tools.data.app.openDeveloperTelegram
import com.geely.ex2.tools.ui.clickableWithSystemSound
import com.geely.ex2.tools.ui.rememberOnClickWithSystemSound
import com.geely.ex2.tools.ui.theme.GeelyEx2ToolsTheme

@Composable
fun AppAboutDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val versionName = remember { getAppVersionName(context) }

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
                text = stringResource(R.string.home_about_title),
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
                Image(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
                Text(
                    text = stringResource(R.string.home_about_version, versionName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_about_developer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            }
        },
    )
}

@Preview(showBackground = true, name = "About Light")
@Composable
private fun AppAboutDialogPreviewLight() {
    GeelyEx2ToolsTheme(darkTheme = false) {
        AppAboutDialog(onDismiss = {})
    }
}

@Preview(showBackground = true, name = "About Dark")
@Composable
private fun AppAboutDialogPreviewDark() {
    GeelyEx2ToolsTheme(darkTheme = true) {
        AppAboutDialog(onDismiss = {})
    }
}
