package com.gonzalo.robotcontroller.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gonzalo.robotcontroller.domain.model.RobotCommand

@Composable
fun OverlayControls(
    visible: Boolean,
    isGamepadConnected: Boolean,
    speed: Int,
    onSpeedChange: (Int) -> Unit,
    onSendCommand: (RobotCommand) -> Unit,
    enabled: Boolean,
    gamepadJoystickPosition: Pair<Float, Float>,
    controlMode: ControlMode,
    modifier: Modifier = Modifier
) {
    val alpha = if (isGamepadConnected) 0.9f else 0.7f

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    when (controlMode) {
                        ControlMode.DPad -> OverlayDPadControls(
                            onSendCommand = onSendCommand,
                            enabled = enabled
                        )
                        ControlMode.Joystick -> OverlayJoystickControl(
                            onSendCommand = onSendCommand,
                            enabled = enabled,
                            gamepadPosition = gamepadJoystickPosition
                        )
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    OverlaySpeedControl(
                        speed = speed,
                        onSpeedChange = onSpeedChange,
                        enabled = enabled
                    )
                }
            }

            if (isGamepadConnected) {
                Text(
                    text = "L1 to hide",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .background(
                            Color.Black.copy(alpha = 0.4f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun OverlayDPadControls(
    onSendCommand: (RobotCommand) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val buttonModifier = Modifier.size(70.dp)
    val buttonColors = ButtonDefaults.filledTonalButtonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
    )
    val stopButtonColors = ButtonDefaults.filledTonalButtonColors(
        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilledTonalButton(
            onClick = { onSendCommand(RobotCommand.Forward) },
            enabled = enabled,
            modifier = buttonModifier,
            shape = CircleShape,
            colors = buttonColors
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Forward")
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { onSendCommand(RobotCommand.Left) },
                enabled = enabled,
                modifier = buttonModifier,
                shape = CircleShape,
                colors = buttonColors
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Left")
            }

            FilledTonalButton(
                onClick = { onSendCommand(RobotCommand.Stop) },
                enabled = enabled,
                modifier = buttonModifier,
                shape = CircleShape,
                colors = stopButtonColors
            ) {
                Text("STOP", style = MaterialTheme.typography.labelSmall)
            }

            FilledTonalButton(
                onClick = { onSendCommand(RobotCommand.Right) },
                enabled = enabled,
                modifier = buttonModifier,
                shape = CircleShape,
                colors = buttonColors
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Right")
            }
        }

        FilledTonalButton(
            onClick = { onSendCommand(RobotCommand.Backward) },
            enabled = enabled,
            modifier = buttonModifier,
            shape = CircleShape,
            colors = buttonColors
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Backward")
        }
    }
}

@Composable
private fun OverlayJoystickControl(
    onSendCommand: (RobotCommand) -> Unit,
    enabled: Boolean,
    gamepadPosition: Pair<Float, Float>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        JoystickControlLandscape(
            onSendCommand = onSendCommand,
            enabled = enabled,
            gamepadPosition = gamepadPosition
        )
    }
}

@Composable
private fun OverlaySpeedControl(
    speed: Int,
    onSpeedChange: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Speed",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )

        Text(
            text = "$speed%",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Slider(
            value = speed.toFloat(),
            onValueChange = { onSpeedChange(it.toInt()) },
            valueRange = 0f..100f,
            enabled = enabled,
            modifier = Modifier
                .width(200.dp)
                .height(48.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )
    }
}
