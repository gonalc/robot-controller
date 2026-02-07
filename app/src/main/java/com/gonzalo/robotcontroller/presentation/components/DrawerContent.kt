package com.gonzalo.robotcontroller.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gonzalo.robotcontroller.domain.model.ConnectionState

@Composable
fun DrawerContent(
    connectionState: ConnectionState,
    testMode: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onToggleTestMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        HorizontalDivider()

        ConnectionSection(
            connectionState = connectionState,
            onConnect = onConnect,
            onDisconnect = onDisconnect
        )

        HorizontalDivider()

        DebugSection(
            testMode = testMode,
            onToggleTestMode = onToggleTestMode
        )
    }
}

@Composable
private fun ConnectionSection(
    connectionState: ConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Connection",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        val statusText = when (connectionState) {
            is ConnectionState.Connected -> "Connected"
            is ConnectionState.Connecting -> "Connecting..."
            is ConnectionState.Disconnected -> "Disconnected"
            is ConnectionState.Error -> connectionState.message
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConnectionStatusDot(connectionState = connectionState)
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = when (connectionState) {
                    is ConnectionState.Connected -> MaterialTheme.colorScheme.primary
                    is ConnectionState.Error -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }

        Button(
            onClick = {
                if (connectionState is ConnectionState.Connected) {
                    onDisconnect()
                } else {
                    onConnect()
                }
            },
            enabled = connectionState !is ConnectionState.Connecting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (connectionState is ConnectionState.Connected) "Disconnect" else "Connect"
            )
        }
    }
}

@Composable
private fun DebugSection(
    testMode: Boolean,
    onToggleTestMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Debug",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Test Mode",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (testMode) "Controls enabled, no commands sent" else "Commands sent normally",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = testMode,
                onCheckedChange = { onToggleTestMode() }
            )
        }
    }
}
