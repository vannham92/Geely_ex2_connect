package com.geely.ex2.tools.feature.ambient.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geely.ex2.tools.R
import com.geely.ex2.tools.ui.rememberOnClickWithSystemSound
import com.geely.ex2.tools.ui.theme.GeelyEx2ToolsTheme

@Composable
fun AmbientLightInfoDialog(
    onDismiss: () -> Unit,
) {
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
                text = stringResource(R.string.ambient_light_info_title),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.ambient_light_info_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            )
        },
    )
}

@Preview(showBackground = true, name = "Ambient Info Light")
@Composable
private fun AmbientLightInfoDialogPreviewLight() {
    GeelyEx2ToolsTheme(darkTheme = false) {
        AmbientLightInfoDialog(onDismiss = {})
    }
}

@Preview(showBackground = true, name = "Ambient Info Dark")
@Composable
private fun AmbientLightInfoDialogPreviewDark() {
    GeelyEx2ToolsTheme(darkTheme = true) {
        AmbientLightInfoDialog(onDismiss = {})
    }
}
