package com.gonzalo.robotcontroller.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.unit.dp
import com.gonzalo.robotcontroller.domain.model.ConnectionState
import com.gonzalo.robotcontroller.domain.model.RobotCommand
import com.gonzalo.robotcontroller.presentation.components.ConnectionStatusDot
import com.gonzalo.robotcontroller.presentation.components.ControlMode
import com.gonzalo.robotcontroller.presentation.components.ControlModeSelector
import com.gonzalo.robotcontroller.presentation.components.DirectionalControlsCard
import com.gonzalo.robotcontroller.presentation.components.DirectionalControlsLandscape
import com.gonzalo.robotcontroller.presentation.components.DrawerContent
import com.gonzalo.robotcontroller.presentation.components.JoystickControlCard
import com.gonzalo.robotcontroller.presentation.components.JoystickControlLandscape
import com.gonzalo.robotcontroller.presentation.components.SpeedControlCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RobotControlScreen(
    connectionState: ConnectionState,
    testMode: Boolean,
    gamepadJoystickPosition: Pair<Float, Float>,
    speed: Int,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSendCommand: (RobotCommand) -> Unit,
    onSpeedChange: (Int) -> Unit,
    onToggleTestMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var controlMode by remember { mutableStateOf(ControlMode.DPad) }
    val controlsEnabled = connectionState is ConnectionState.Connected || testMode

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                DrawerContent(
                    connectionState = connectionState,
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
                        ConnectionStatusDot(connectionState = connectionState)
                        Spacer(modifier = Modifier.width(16.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { paddingValues ->
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

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
