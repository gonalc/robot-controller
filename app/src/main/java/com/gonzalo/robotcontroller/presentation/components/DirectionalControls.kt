package com.gonzalo.robotcontroller.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gonzalo.robotcontroller.domain.model.RobotCommand

@Composable
fun DirectionalControlsCard(
    onSendCommand: (RobotCommand) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Movement Controls",
                style = MaterialTheme.typography.titleMedium
            )

            DirectionalButtonsLayout(
                onSendCommand = onSendCommand,
                enabled = enabled,
                isLandscape = false
            )
        }
    }
}

@Composable
fun DirectionalControlsLandscape(
    onSendCommand: (RobotCommand) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    DirectionalButtonsLayout(
        onSendCommand = onSendCommand,
        enabled = enabled,
        isLandscape = true,
        modifier = modifier
    )
}

@Composable
private fun DirectionalButtonsLayout(
    onSendCommand: (RobotCommand) -> Unit,
    enabled: Boolean,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val buttonModifier = if (isLandscape) {
        Modifier.size(80.dp)
    } else {
        Modifier.fillMaxWidth().height(60.dp)
    }
    val buttonShape = if (isLandscape) CircleShape else ButtonDefaults.filledTonalShape

    Column(
        modifier = modifier.then(if (!isLandscape) Modifier.fillMaxWidth() else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = { onSendCommand(RobotCommand.Forward) },
            enabled = enabled,
            modifier = buttonModifier,
            shape = buttonShape
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Forward")
        }

        Row(
            modifier = if (!isLandscape) Modifier.fillMaxWidth() else Modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { onSendCommand(RobotCommand.Left) },
                enabled = enabled,
                modifier = if (isLandscape) Modifier.size(80.dp) else Modifier.weight(1f).height(60.dp),
                shape = buttonShape
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Left")
            }

            FilledTonalButton(
                onClick = { onSendCommand(RobotCommand.Stop) },
                enabled = enabled,
                modifier = if (isLandscape) Modifier.size(80.dp) else Modifier.weight(1f).height(60.dp),
                shape = buttonShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    "STOP",
                    style = if (isLandscape) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge
                )
            }

            FilledTonalButton(
                onClick = { onSendCommand(RobotCommand.Right) },
                enabled = enabled,
                modifier = if (isLandscape) Modifier.size(80.dp) else Modifier.weight(1f).height(60.dp),
                shape = buttonShape
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Right")
            }
        }

        FilledTonalButton(
            onClick = { onSendCommand(RobotCommand.Backward) },
            enabled = enabled,
            modifier = buttonModifier,
            shape = buttonShape
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Backward")
        }
    }
}
