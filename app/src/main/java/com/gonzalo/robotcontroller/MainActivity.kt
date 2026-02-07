package com.gonzalo.robotcontroller

import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.gonzalo.robotcontroller.data.preferences.SettingsDataStore
import com.gonzalo.robotcontroller.data.repository.RobotRepository
import com.gonzalo.robotcontroller.data.websocket.WebSocketClient
import com.gonzalo.robotcontroller.domain.model.RobotCommand
import com.gonzalo.robotcontroller.presentation.RobotControlScreen
import com.gonzalo.robotcontroller.presentation.RobotControlViewModel
import com.gonzalo.robotcontroller.presentation.RobotControlViewModelFactory
import com.gonzalo.robotcontroller.ui.theme.RobotControllerTheme

class MainActivity : ComponentActivity() {

    private var lastHatX = 0f
    private var lastHatY = 0f

    // Speed control with triggers
    private var rtSpeedJob: Job? = null
    private var ltSpeedJob: Job? = null
    private val speedAdjustIntervalMs = 150L

    private val viewModel: RobotControlViewModel by viewModels {
        val webSocketClient = WebSocketClient()
        val repository = RobotRepository(webSocketClient)
        val settingsDataStore = SettingsDataStore(applicationContext)
        RobotControlViewModelFactory(repository, settingsDataStore)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RobotControllerTheme {
                val connectionState by viewModel.connectionState.collectAsState()
                val testMode by viewModel.testMode.collectAsState()
                val gamepadJoystickPosition by viewModel.gamepadJoystickPosition.collectAsState()
                val speed by viewModel.speed.collectAsState()

                RobotControlScreen(
                    connectionState = connectionState,
                    testMode = testMode,
                    gamepadJoystickPosition = gamepadJoystickPosition,
                    speed = speed,
                    onConnect = { viewModel.connect() },
                    onDisconnect = { viewModel.disconnect() },
                    onSendCommand = { command -> viewModel.sendCommand(command) },
                    onSpeedChange = { viewModel.setSpeed(it) },
                    onToggleTestMode = { viewModel.toggleTestMode() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // --- Bluetooth gamepad: D-pad buttons ---

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val command = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> RobotCommand.Forward
            KeyEvent.KEYCODE_DPAD_DOWN -> RobotCommand.Backward
            KeyEvent.KEYCODE_DPAD_LEFT -> RobotCommand.Left
            KeyEvent.KEYCODE_DPAD_RIGHT -> RobotCommand.Right
            else -> return super.onKeyDown(keyCode, event)
        }
        if (event?.repeatCount == 0) {
            viewModel.sendCommand(command)
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode in intArrayOf(
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT
            )) {
            viewModel.sendCommand(RobotCommand.Stop)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    // --- Bluetooth gamepad: left analog stick + hat switch ---

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event == null) return super.onGenericMotionEvent(event)

        // Log all axis values to identify buttons/triggers
        val axes = listOf(
            "X" to MotionEvent.AXIS_X,
            "Y" to MotionEvent.AXIS_Y,
            "Z" to MotionEvent.AXIS_Z,
            "RZ" to MotionEvent.AXIS_RZ,
            "LT" to MotionEvent.AXIS_LTRIGGER,
            "RT" to MotionEvent.AXIS_RTRIGGER,
            "HAT_X" to MotionEvent.AXIS_HAT_X,
            "HAT_Y" to MotionEvent.AXIS_HAT_Y,
            "BRAKE" to MotionEvent.AXIS_BRAKE,
            "GAS" to MotionEvent.AXIS_GAS
        )
        val nonZero = axes.mapNotNull { (name, axis) ->
            val value = event.getAxisValue(axis)
            if (kotlin.math.abs(value) > 0.01f) "$name=${"%.2f".format(value)}" else null
        }
        if (nonZero.isNotEmpty()) {
            Log.d("GamepadDebug", "Axes: ${nonZero.joinToString(", ")}")
        }

        if (event.action != MotionEvent.ACTION_MOVE) {
            return super.onGenericMotionEvent(event)
        }
        val sourceIsJoystick = event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        if (!sourceIsJoystick) return super.onGenericMotionEvent(event)

        // Left analog stick
        val stickX = event.getAxisValue(MotionEvent.AXIS_X)
        val stickY = event.getAxisValue(MotionEvent.AXIS_Y)
        viewModel.handleGamepadStick(stickX, stickY)

        // Hat switch (some controllers report D-pad this way)
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        if (hatX != lastHatX || hatY != lastHatY) {
            lastHatX = hatX
            lastHatY = hatY
            val command = when {
                hatY < 0f -> RobotCommand.Forward
                hatY > 0f -> RobotCommand.Backward
                hatX < 0f -> RobotCommand.Left
                hatX > 0f -> RobotCommand.Right
                else -> RobotCommand.Stop
            }
            viewModel.sendCommand(command)
        }

        // Triggers for speed control (RT = increase, LT = decrease)
        val rtValue = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)
        val ltValue = event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
        val rtPressed = rtValue > 0.5f
        val ltPressed = ltValue > 0.5f

        // Start/stop continuous speed adjustment coroutines
        if (rtPressed && rtSpeedJob == null) {
            rtSpeedJob = lifecycleScope.launch {
                while (true) {
                    viewModel.adjustSpeed(5)
                    delay(speedAdjustIntervalMs)
                }
            }
        } else if (!rtPressed && rtSpeedJob != null) {
            rtSpeedJob?.cancel()
            rtSpeedJob = null
        }

        if (ltPressed && ltSpeedJob == null) {
            ltSpeedJob = lifecycleScope.launch {
                while (true) {
                    viewModel.adjustSpeed(-5)
                    delay(speedAdjustIntervalMs)
                }
            }
        } else if (!ltPressed && ltSpeedJob != null) {
            ltSpeedJob?.cancel()
            ltSpeedJob = null
        }

        return true
    }
}