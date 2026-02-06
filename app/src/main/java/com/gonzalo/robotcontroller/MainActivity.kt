package com.gonzalo.robotcontroller

import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
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

    private val viewModel: RobotControlViewModel by viewModels {
        val webSocketClient = WebSocketClient()
        val repository = RobotRepository(webSocketClient, lifecycleScope)
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

                RobotControlScreen(
                    connectionState = connectionState,
                    testMode = testMode,
                    gamepadJoystickPosition = gamepadJoystickPosition,
                    onConnect = { viewModel.connect() },
                    onDisconnect = { viewModel.disconnect() },
                    onSendCommand = { command -> viewModel.sendCommand(command) },
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
        Log.d("GamepadDebug", "onGenericMotionEvent: $event")
        if (event == null || event.action != MotionEvent.ACTION_MOVE) {
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

        return true
    }
}