package com.geely.ex2.tools.ui.components

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.geely.ex2.tools.R
import com.geely.ex2.tools.ui.rememberOnClickWithSystemSound
import java.util.Locale

@Composable
fun FlymeTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var hour by remember(initialHour, initialMinute) { mutableIntStateOf(initialHour) }
    var minute by remember(initialHour, initialMinute) { mutableIntStateOf(initialMinute) }
    val onConfirmWithSound = rememberOnClickWithSystemSound { onConfirm(hour, minute) }
    val onDismissWithSound = rememberOnClickWithSystemSound(onDismiss)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                AndroidView(
                    modifier = Modifier.weight(1f),
                    factory = { context ->
                        NumberPicker(context).apply {
                            minValue = 0
                            maxValue = 23
                            value = initialHour.coerceIn(0, 23)
                            wrapSelectorWheel = true
                            setFormatter { value ->
                                String.format(Locale.US, "%02d h", value)
                            }
                            setOnValueChangedListener { _, _, newValue ->
                                hour = newValue
                            }
                        }
                    },
                    update = { picker ->
                        if (picker.value != hour) {
                            picker.value = hour
                        }
                    },
                )
                AndroidView(
                    modifier = Modifier.weight(1f),
                    factory = { context ->
                        NumberPicker(context).apply {
                            minValue = 0
                            maxValue = 59
                            value = initialMinute.coerceIn(0, 59)
                            wrapSelectorWheel = true
                            setFormatter { value ->
                                String.format(Locale.US, "%02d min", value)
                            }
                            setOnValueChangedListener { _, _, newValue ->
                                minute = newValue
                            }
                        }
                    },
                    update = { picker ->
                        if (picker.value != minute) {
                            picker.value = minute
                        }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirmWithSound) {
                Text(
                    text = stringResource(R.string.ambient_light_time_picker_confirm),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissWithSound) {
                Text(
                    text = stringResource(R.string.ambient_light_time_picker_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
