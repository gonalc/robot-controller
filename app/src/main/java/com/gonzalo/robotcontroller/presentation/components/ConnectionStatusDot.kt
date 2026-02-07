package com.gonzalo.robotcontroller.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gonzalo.robotcontroller.domain.model.ConnectionState

@Composable
fun ConnectionStatusDot(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val color = when (connectionState) {
        is ConnectionState.Connected -> MaterialTheme.colorScheme.primary
        is ConnectionState.Connecting -> MaterialTheme.colorScheme.tertiary
        is ConnectionState.Disconnected -> MaterialTheme.colorScheme.outline
        is ConnectionState.Error -> MaterialTheme.colorScheme.error
    }

    Box(
        modifier = modifier
            .size(12.dp)
            .background(color = color, shape = CircleShape)
    )
}
