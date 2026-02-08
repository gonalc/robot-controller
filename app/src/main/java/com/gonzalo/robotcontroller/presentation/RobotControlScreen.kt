package com.gonzalo.robotcontroller.presentation

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.gonzalo.robotcontroller.domain.model.CaptureResponse
import com.gonzalo.robotcontroller.domain.model.ConnectionState
import com.gonzalo.robotcontroller.domain.model.RobotCommand
import com.gonzalo.robotcontroller.domain.model.StreamingState
import com.gonzalo.robotcontroller.presentation.components.CaptureImageDialog
import com.gonzalo.robotcontroller.presentation.components.ConnectionStatusDot
import com.gonzalo.robotcontroller.presentation.components.ControlMode
import com.gonzalo.robotcontroller.presentation.components.ControlModeSelector
import com.gonzalo.robotcontroller.presentation.components.DirectionalControlsCard
import com.gonzalo.robotcontroller.presentation.components.DirectionalControlsLandscape
import com.gonzalo.robotcontroller.presentation.components.DrawerContent
import com.gonzalo.robotcontroller.presentation.components.JoystickControlCard
import com.gonzalo.robotcontroller.presentation.components.JoystickControlLandscape
import com.gonzalo.robotcontroller.presentation.components.OverlayControls
import com.gonzalo.robotcontroller.presentation.components.SpeedControlCard
import com.gonzalo.robotcontroller.presentation.components.VideoStreamView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RobotControlScreen(
    connectionState: ConnectionState,
    serverUrl: String,
    testMode: Boolean,
    gamepadJoystickPosition: Pair<Float, Float>,
    speed: Int,
    capturedImage: CaptureResponse?,
    isCapturing: Boolean,
    streamingState: StreamingState,
    currentFrame: Bitmap?,
    isGamepadConnected: Boolean,
    controlsVisible: Boolean,
    streamingEnabled: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSendCommand: (RobotCommand) -> Unit,
    onSpeedChange: (Int) -> Unit,
    onToggleTestMode: () -> Unit,
    onCapture: (width: Int, height: Int) -> Unit,
    onDismissCapturedImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var controlMode by remember { mutableStateOf(ControlMode.DPad) }
    val controlsEnabled = connectionState is ConnectionState.Connected || testMode

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val showFullscreenStreaming = isLandscape && streamingEnabled &&
        (streamingState is StreamingState.Streaming || streamingState is StreamingState.Connecting || testMode)

    if (showFullscreenStreaming) {
        StreamingLayout(
            streamingState = streamingState,
            currentFrame = currentFrame,
            controlMode = controlMode,
            controlsEnabled = controlsEnabled,
            controlsVisible = controlsVisible || !isGamepadConnected,
            isGamepadConnected = isGamepadConnected,
            speed = speed,
            onSpeedChange = onSpeedChange,
            onSendCommand = onSendCommand,
            gamepadJoystickPosition = gamepadJoystickPosition,
            connectionState = connectionState,
            modifier = modifier.fillMaxSize()
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .widthIn(max = 320.dp)
                ) {
                    DrawerContent(
                        connectionState = connectionState,
                        serverUrl = serverUrl,
                        testMode = testMode,
                        onConnect = onConnect,
                        onDisconnect = onDisconnect,
                        onToggleTestMode = onToggleTestMode
                    )
                }
            },
            modifier = modifier
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Robot Controller") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    val widthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() } and 1.inv()
                                    val heightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() } and 1.inv()
                                    onCapture(widthPx, heightPx)
                                },
                                enabled = controlsEnabled && !isCapturing
                            ) {
                                if (isCapturing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Capture Photo",
                                        tint = if (controlsEnabled) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                            ConnectionStatusDot(connectionState = connectionState)
                            Spacer(modifier = Modifier.width(16.dp))
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            ) { paddingValues ->
                if (isLandscape) {
                    LandscapeLayout(
                        controlMode = controlMode,
                        onModeSelected = { controlMode = it },
                        controlsEnabled = controlsEnabled,
                        speed = speed,
                        onSpeedChange = onSpeedChange,
                        onSendCommand = onSendCommand,
                        gamepadJoystickPosition = gamepadJoystickPosition,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                    )
                } else {
                    PortraitLayout(
                        controlMode = controlMode,
                        onModeSelected = { controlMode = it },
                        controlsEnabled = controlsEnabled,
                        speed = speed,
                        onSpeedChange = onSpeedChange,
                        onSendCommand = onSendCommand,
                        gamepadJoystickPosition = gamepadJoystickPosition,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                    )
                }
            }
        }
    }

    // Show captured image dialog
    capturedImage?.let { capture ->
        CaptureImageDialog(
            captureResponse = capture,
            onDismiss = onDismissCapturedImage
        )
    }
}

@Composable
private fun PortraitLayout(
    controlMode: ControlMode,
    onModeSelected: (ControlMode) -> Unit,
    controlsEnabled: Boolean,
    speed: Int,
    onSpeedChange: (Int) -> Unit,
    onSendCommand: (RobotCommand) -> Unit,
    gamepadJoystickPosition: Pair<Float, Float>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SpeedControlCard(
            speed = speed,
            onSpeedChange = onSpeedChange,
            enabled = controlsEnabled
        )

        ControlModeSelector(
            selectedMode = controlMode,
            onModeSelected = onModeSelected
        )

        when (controlMode) {
            ControlMode.DPad -> DirectionalControlsCard(
                onSendCommand = onSendCommand,
                enabled = controlsEnabled
            )
            ControlMode.Joystick -> JoystickControlCard(
                onSendCommand = onSendCommand,
                enabled = controlsEnabled,
                gamepadPosition = gamepadJoystickPosition
            )
        }
    }
}

@Composable
private fun LandscapeLayout(
    controlMode: ControlMode,
    onModeSelected: (ControlMode) -> Unit,
    controlsEnabled: Boolean,
    speed: Int,
    onSpeedChange: (Int) -> Unit,
    onSendCommand: (RobotCommand) -> Unit,
    gamepadJoystickPosition: Pair<Float, Float>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Movement controls
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (controlMode) {
                ControlMode.DPad -> DirectionalControlsLandscape(
                    onSendCommand = onSendCommand,
                    enabled = controlsEnabled
                )
                ControlMode.Joystick -> JoystickControlLandscape(
                    onSendCommand = onSendCommand,
                    enabled = controlsEnabled,
                    gamepadPosition = gamepadJoystickPosition
                )
            }
        }

        // Right side: Speed and mode selector
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SpeedControlCard(
                speed = speed,
                onSpeedChange = onSpeedChange,
                enabled = controlsEnabled
            )

            ControlModeSelector(
                selectedMode = controlMode,
                onModeSelected = onModeSelected
            )
        }
    }
}

@Composable
private fun StreamingLayout(
    streamingState: StreamingState,
    currentFrame: Bitmap?,
    controlMode: ControlMode,
    controlsEnabled: Boolean,
    controlsVisible: Boolean,
    isGamepadConnected: Boolean,
    speed: Int,
    onSpeedChange: (Int) -> Unit,
    onSendCommand: (RobotCommand) -> Unit,
    gamepadJoystickPosition: Pair<Float, Float>,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        VideoStreamView(
            streamingState = streamingState,
            currentFrame = currentFrame,
            modifier = Modifier.fillMaxSize()
        )

        OverlayControls(
            visible = controlsVisible,
            isGamepadConnected = isGamepadConnected,
            speed = speed,
            onSpeedChange = onSpeedChange,
            onSendCommand = onSendCommand,
            enabled = controlsEnabled,
            gamepadJoystickPosition = gamepadJoystickPosition,
            controlMode = controlMode,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        )

        // Connection status indicator in top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .systemBarsPadding()
                .padding(16.dp)
        ) {
            ConnectionStatusDot(connectionState = connectionState)
        }
    }
}
