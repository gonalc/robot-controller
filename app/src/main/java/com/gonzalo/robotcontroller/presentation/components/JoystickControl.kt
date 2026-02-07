package com.gonzalo.robotcontroller.presentation.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gonzalo.robotcontroller.domain.model.RobotCommand
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun JoystickControlCard(
    onSendCommand: (RobotCommand) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    gamepadPosition: Pair<Float, Float> = Pair(0f, 0f)
) {
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

            JoystickCore(
                onSendCommand = onSendCommand,
                enabled = enabled,
                gamepadPosition = gamepadPosition,
                joystickSize = 200.dp,
                knobRadius = 30.dp
            )
        }
    }
}

@Composable
fun JoystickControlLandscape(
    onSendCommand: (RobotCommand) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    gamepadPosition: Pair<Float, Float> = Pair(0f, 0f)
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        JoystickCore(
            onSendCommand = onSendCommand,
            enabled = enabled,
            gamepadPosition = gamepadPosition,
            joystickSize = 220.dp,
            knobRadius = 35.dp
        )
    }
}

@Composable
private fun JoystickCore(
    onSendCommand: (RobotCommand) -> Unit,
    enabled: Boolean,
    gamepadPosition: Pair<Float, Float>,
    joystickSize: Dp,
    knobRadius: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val joystickRadiusPx = with(density) { (joystickSize / 2).toPx() }
    val knobRadiusPx = with(density) { knobRadius.toPx() }
    val maxOffset = joystickRadiusPx - knobRadiusPx

    var knobOffsetX by remember { mutableFloatStateOf(0f) }
    var knobOffsetY by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var springJob by remember { mutableStateOf<Job?>(null) }

    var normalizedX by remember { mutableFloatStateOf(0f) }
    var normalizedY by remember { mutableFloatStateOf(0f) }

    // Update knob position from gamepad when not dragging on screen
    LaunchedEffect(gamepadPosition, isDragging) {
        if (!isDragging) {
            val (gpX, gpY) = gamepadPosition
            normalizedX = gpX
            normalizedY = gpY
            knobOffsetX = gpX * maxOffset
            knobOffsetY = -gpY * maxOffset
        }
    }

    // Reset when disabled
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

    // Throttled command sending at ~20 Hz (only for touch input)
    LaunchedEffect(enabled, isDragging) {
        if (!enabled || !isDragging) return@LaunchedEffect
        var lastSentX = 0f
        var lastSentY = 0f
        while (isDragging) {
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

    val isGamepadActive = !isDragging && (gamepadPosition.first != 0f || gamepadPosition.second != 0f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
                                animate(
                                    startX,
                                    0f,
                                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
                                ) { value, _ ->
                                    knobOffsetX = value
                                }
                            }
                            launch {
                                animate(
                                    startY,
                                    0f,
                                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
                                ) { value, _ ->
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
                color = when {
                    isDragging -> MaterialTheme.colorScheme.primary
                    isGamepadActive -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                shadowElevation = if (isDragging || isGamepadActive) 8.dp else 4.dp
            ) {}
        }

        Text(
            text = "x: ${"%.2f".format(normalizedX)}, y: ${"%.2f".format(normalizedY)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun clampToCircle(offset: Offset, maxRadius: Float): Offset {
    val distance = offset.getDistance()
    return if (distance <= maxRadius) offset
    else offset * (maxRadius / distance)
}
