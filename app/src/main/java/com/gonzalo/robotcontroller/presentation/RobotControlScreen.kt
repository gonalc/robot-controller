package com.gonzalo.robotcontroller.presentation

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gonzalo.robotcontroller.domain.model.ConnectionState
import com.gonzalo.robotcontroller.domain.model.RobotCommand
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ControlMode {
    DPad,
    Joystick
}

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
    var controlMode by remember { mutableStateOf(ControlMode.DPad) }

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

            ControlModeSelector(
                selectedMode = controlMode,
                onModeSelected = { controlMode = it }
            )

            when (controlMode) {
                ControlMode.DPad -> DirectionalControlsCard(
                    onSendCommand = onSendCommand,
                    enabled = connectionState is ConnectionState.Connected
                )
                ControlMode.Joystick -> JoystickControlCard(
                    onSendCommand = onSendCommand,
                    enabled = connectionState is ConnectionState.Connected
                )
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlModeSelector(
    selectedMode: ControlMode,
    onModeSelected: (ControlMode) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        ControlMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = ControlMode.entries.size
                )
            ) {
                Text(
                    when (mode) {
                        ControlMode.DPad -> "D-Pad"
                        ControlMode.Joystick -> "Joystick"
                    }
                )
            }
        }
    }
}

@Composable
fun JoystickControlCard(
    onSendCommand: (RobotCommand) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val joystickSize = 200.dp
    val knobRadius = 30.dp

    val density = LocalDensity.current
    val joystickRadiusPx = with(density) { (joystickSize / 2).toPx() }
    val knobRadiusPx = with(density) { knobRadius.toPx() }
    val maxOffset = joystickRadiusPx - knobRadiusPx

    var knobOffsetX by remember { mutableFloatStateOf(0f) }
    var knobOffsetY by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var springJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    var normalizedX by remember { mutableFloatStateOf(0f) }
    var normalizedY by remember { mutableFloatStateOf(0f) }

    // Reset when disabled (e.g. connection drops mid-drag)
    LaunchedEffect(enabled) {
        if (!enabled) {
            springJob?.cancel()
            isDragging = false
            normalizedX = 0f
            normalizedY = 0f
            knobOffsetX = 0f
            knobOffsetY = 0f
        }
    }

    // Throttled command sending at ~20 Hz
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        var lastSentX = 0f
        var lastSentY = 0f
        while (true) {
            delay(50L)
            val nx = normalizedX
            val ny = normalizedY
            if (nx != lastSentX || ny != lastSentY) {
                onSendCommand(RobotCommand.Joystick(nx, ny))
                lastSentX = nx
                lastSentY = ny
            }
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Joystick Controls",
                style = MaterialTheme.typography.titleMedium
            )

            Box(
                modifier = Modifier
                    .size(joystickSize)
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        val center = Offset(size.width / 2f, size.height / 2f)

                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            springJob?.cancel()
                            isDragging = true

                            val initial = clampToCircle(down.position - center, maxOffset)
                            knobOffsetX = initial.x
                            knobOffsetY = initial.y
                            normalizedX = initial.x / maxOffset
                            normalizedY = -(initial.y / maxOffset)

                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null || !change.pressed) break
                                change.consume()

                                val clamped = clampToCircle(change.position - center, maxOffset)
                                knobOffsetX = clamped.x
                                knobOffsetY = clamped.y
                                normalizedX = clamped.x / maxOffset
                                normalizedY = -(clamped.y / maxOffset)
                            }

                            // Release: send immediate stop, then spring back visually
                            isDragging = false
                            normalizedX = 0f
                            normalizedY = 0f
                            onSendCommand(RobotCommand.Joystick(0f, 0f))

                            val startX = knobOffsetX
                            val startY = knobOffsetY
                            springJob = coroutineScope.launch {
                                launch {
                                    animate(startX, 0f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)) { value, _ ->
                                        knobOffsetX = value
                                    }
                                }
                                launch {
                                    animate(startY, 0f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)) { value, _ ->
                                        knobOffsetY = value
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                )
                Surface(
                    modifier = Modifier
                        .size(knobRadius * 2)
                        .offset { IntOffset(knobOffsetX.roundToInt(), knobOffsetY.roundToInt()) },
                    shape = CircleShape,
                    color = if (isDragging) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = if (isDragging) 8.dp else 4.dp
                ) {}
            }

            Text(
                text = "x: ${"%.2f".format(normalizedX)}, y: ${"%.2f".format(normalizedY)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun clampToCircle(offset: Offset, maxRadius: Float): Offset {
    val distance = offset.getDistance()
    return if (distance <= maxRadius) offset
    else offset * (maxRadius / distance)
}
