package com.geely.ex2.tools.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geely.ex2.tools.R
import com.geely.ex2.tools.data.network.CarTcpServer
import com.geely.ex2.tools.data.network.CarTransport
import com.geely.ex2.tools.ui.components.FlymeSettingsSection

/**
 * Cài đặt → "Kết nối điện thoại": xe đang nối với phone qua Bluetooth hay WiFi.
 *
 * Đọc thẳng [CarTcpServer.connectedPeers] — server là singleton sống cùng process, cập nhật
 * ngay khi client vào/ra nên không cần poll.
 */
@Composable
fun PhoneLinkSection(modifier: Modifier = Modifier) {
    val peers by CarTcpServer.connectedPeers.collectAsStateWithLifecycle()

    FlymeSettingsSection(title = stringResource(R.string.settings_section_phone_link), modifier = modifier) {
        CarTransport.entries.forEachIndexed { index, transport ->
            val peer = peers.firstOrNull { it.transport == transport }
            PhoneLinkRow(
                title = stringResource(
                    when (transport) {
                        CarTransport.BLUETOOTH -> R.string.settings_phone_link_bluetooth
                        CarTransport.WIFI -> R.string.settings_phone_link_wifi
                    },
                ),
                summary = peer?.label ?: stringResource(R.string.settings_phone_link_idle),
                connected = peer != null,
                showDivider = index < CarTransport.entries.lastIndex,
            )
        }
    }
}

@Composable
private fun PhoneLinkRow(
    title: String,
    summary: String,
    connected: Boolean,
    showDivider: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Chấm trạng thái: xanh = đang nối, xám = trống.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (connected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = CircleShape,
                        ),
                )
                Text(
                    text = stringResource(
                        if (connected) R.string.settings_phone_link_state_on
                        else R.string.settings_phone_link_state_off,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (connected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
            )
        }
    }
}
