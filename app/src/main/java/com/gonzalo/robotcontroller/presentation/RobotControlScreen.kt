package com.gonzalo.robotcontroller.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gonzalo.robotcontroller.domain.model.ConnectionState
import com.gonzalo.robotcontroller.domain.model.RobotCommand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RobotControlScreen(
    connectionState: ConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSendCommand: (RobotCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSpeed by remember { mutableIntStateOf(50) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Robot Controller") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ConnectionStatusCard(
                connectionState = connectionState,
                onConnect = onConnect,
                onDisconnect = onDisconnect
            )

            SpeedControlCard(
                speed = currentSpeed,
                onSpeedChange = { newSpeed ->
                    currentSpeed = newSpeed
                    onSendCommand(RobotCommand.Speed(newSpeed))
                },
                enabled = connectionState is ConnectionState.Connected
            )

            DirectionalControlsCard(
                onSendCommand = onSendCommand,
                enabled = connectionState is ConnectionState.Connected
            )
        }
    }
}

@Composable
fun ConnectionStatusCard(
    connectionState: ConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Connection Status",
                style = MaterialTheme.typography.titleMedium
            )

            val statusText = when (connectionState) {
                is ConnectionState.Connected -> "Connected"
                is ConnectionState.Connecting -> "Connecting..."
                is ConnectionState.Disconnected -> "Disconnected"
                is ConnectionState.Error -> connectionState.message
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = when (connectionState) {
                    is ConnectionState.Connected -> MaterialTheme.colorScheme.primary
                    is ConnectionState.Error -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            Button(
                onClick = {
                    if (connectionState is ConnectionState.Connected) {
                        onDisconnect()
                    } else {
                        onConnect()
                    }
                },
                enabled = connectionState !is ConnectionState.Connecting
            ) {
                Text(
                    if (connectionState is ConnectionState.Connected) "Disconnect" else "Connect"
                )
            }
        }
    }
}

@Composable
fun SpeedControlCard(
    speed: Int,
    onSpeedChange: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Speed Control",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "$speed%",
                style = MaterialTheme.typography.headlineMedium
            )

            Slider(
                value = speed.toFloat(),
                onValueChange = { onSpeedChange(it.toInt()) },
                valueRange = 0f..100f,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

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

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onSendCommand(RobotCommand.Forward) },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Forward")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { onSendCommand(RobotCommand.Left) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f).height(60.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Left")
                    }

                    FilledTonalButton(
                        onClick = { onSendCommand(RobotCommand.Stop) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f).height(60.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text("STOP")
                    }

                    FilledTonalButton(
                        onClick = { onSendCommand(RobotCommand.Right) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f).height(60.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Right")
                    }
                }

                FilledTonalButton(
                    onClick = { onSendCommand(RobotCommand.Backward) },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Backward")
                }
            }
        }
    }
}
