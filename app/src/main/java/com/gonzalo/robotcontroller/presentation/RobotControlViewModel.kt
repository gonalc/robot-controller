package com.gonzalo.robotcontroller.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gonzalo.robotcontroller.data.preferences.SettingsDataStore
import com.gonzalo.robotcontroller.data.repository.RobotRepository
import com.gonzalo.robotcontroller.data.repository.StreamingRepository
import com.gonzalo.robotcontroller.data.sound.SoundManager
import com.gonzalo.robotcontroller.domain.model.CaptureResponse
import com.gonzalo.robotcontroller.domain.model.ConnectionState
import com.gonzalo.robotcontroller.domain.model.RobotCommand
import com.gonzalo.robotcontroller.domain.model.RobotSettings
import com.gonzalo.robotcontroller.domain.model.StreamingState
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RobotControlViewModel(
    private val repository: RobotRepository,
    private val settingsDataStore: SettingsDataStore,
    private val soundManager: SoundManager,
    private val streamingRepository: StreamingRepository
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    private val _testMode = MutableStateFlow(false)
    val testMode: StateFlow<Boolean> = _testMode.asStateFlow()

    val streamingState: StateFlow<StreamingState> = streamingRepository.streamingState
    val currentFrame: StateFlow<Bitmap?> = streamingRepository.currentFrame

    private val _isGamepadConnected = MutableStateFlow(false)
    val isGamepadConnected: StateFlow<Boolean> = _isGamepadConnected.asStateFlow()

    private val _controlsVisible = MutableStateFlow(true)
    val controlsVisible: StateFlow<Boolean> = _controlsVisible.asStateFlow()

    // Gamepad joystick position for UI display
    private val _gamepadJoystickPosition = MutableStateFlow(Pair(0f, 0f))
    val gamepadJoystickPosition: StateFlow<Pair<Float, Float>> = _gamepadJoystickPosition.asStateFlow()

    // Speed control
    private val _speed = MutableStateFlow(50)
    val speed: StateFlow<Int> = _speed.asStateFlow()

    // Track if robot is currently moving
    private var isMoving = false

    // Captured image
    private val _capturedImage = MutableStateFlow<CaptureResponse?>(null)
    val capturedImage: StateFlow<CaptureResponse?> = _capturedImage.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    val settings: StateFlow<RobotSettings> = settingsDataStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RobotSettings()
    )

    init {
        // Observe connection state for engine start sound and streaming
        viewModelScope.launch {
            var wasConnected = false
            connectionState.collect { state ->
                if (state is ConnectionState.Connected && !wasConnected) {
                    soundManager.playEngineStart()
                    // Auto-start streaming when connected
                    val currentSettings = settings.value
                    if (currentSettings.streamingEnabled) {
                        streamingRepository.startStream(
                            currentSettings.serverUrl,
                            currentSettings.streamPort,
                            currentSettings.streamPath
                        )
                    }
                } else if (state !is ConnectionState.Connected && wasConnected) {
                    // Stop streaming when disconnected
                    if (!_testMode.value) {
                        streamingRepository.stopStream()
                    }
                }
                wasConnected = state is ConnectionState.Connected
            }
        }

        viewModelScope.launch {
            settings.collect { newSettings ->
                repository.updateSettings(newSettings)
            }
        }

        viewModelScope.launch {
            repository.captureResponse.collect { response ->
                _isCapturing.value = false
                _capturedImage.value = response
            }
        }
    }

    fun captureImage(width: Int, height: Int) {
        _isCapturing.value = true
        sendCommand(RobotCommand.Capture(width, height))
    }

    fun dismissCapturedImage() {
        _capturedImage.value = null
    }

    fun setSpeed(value: Int) {
        val newSpeed = value.coerceIn(0, 100)
        val oldSpeed = _speed.value
        if (oldSpeed != newSpeed) {
            // Play speeding up sound when speed increases
            if (newSpeed > oldSpeed) {
                soundManager.playSpeedingUp()
            }
            _speed.value = newSpeed
            sendCommand(RobotCommand.Speed(newSpeed))
        }
    }

    fun adjustSpeed(delta: Int) {
        setSpeed(_speed.value + delta)
    }

    fun toggleTestMode() {
        val newTestMode = !_testMode.value
        _testMode.value = newTestMode

        if (newTestMode) {
            streamingRepository.startMockStream()
        } else {
            streamingRepository.stopStream()
            // If connected, restart real stream
            val currentSettings = settings.value
            if (connectionState.value is ConnectionState.Connected && currentSettings.streamingEnabled) {
                streamingRepository.startStream(
                    currentSettings.serverUrl,
                    currentSettings.streamPort,
                    currentSettings.streamPath
                )
            }
        }
    }

    fun toggleControlsVisibility() {
        _controlsVisible.value = !_controlsVisible.value
    }

    fun updateGamepadConnected(connected: Boolean) {
        _isGamepadConnected.value = connected
        // When gamepad connects, hide controls by default
        if (connected) {
            _controlsVisible.value = false
        } else {
            _controlsVisible.value = true
        }
    }

    fun connect() {
        repository.connect(settings.value.serverUrl)
    }

    fun disconnect() {
        soundManager.stopEngineRunning()
        isMoving = false
        repository.disconnect()
    }

    fun sendCommand(command: RobotCommand) {
        if (_testMode.value) return
        repository.sendCommand(command)

        // Handle engine running sound based on movement
        when (command) {
            is RobotCommand.Forward,
            is RobotCommand.Backward,
            is RobotCommand.Left,
            is RobotCommand.Right -> {
                if (!isMoving) {
                    isMoving = true
                    soundManager.startEngineRunning()
                }
            }
            is RobotCommand.Stop -> {
                if (isMoving) {
                    isMoving = false
                    soundManager.stopEngineRunning()
                }
            }
            is RobotCommand.Joystick -> {
                val magnitude = kotlin.math.sqrt(command.x * command.x + command.y * command.y)
                if (magnitude > 0.1f) {
                    if (!isMoving) {
                        isMoving = true
                        soundManager.startEngineRunning()
                    }
                } else {
                    if (isMoving) {
                        isMoving = false
                        soundManager.stopEngineRunning()
                    }
                }
            }
            else -> { /* Speed command - no movement sound change */ }
        }
    }

    // --- Gamepad analog stick support ---

    private var lastStickSendTime = 0L
    private var lastStickX = 0f
    private var lastStickY = 0f

    fun handleGamepadStick(rawX: Float, rawY: Float) {
        val deadZone = 0.1f
        val x = if (abs(rawX) < deadZone) 0f else rawX.coerceIn(-1f, 1f)
        val y = if (abs(rawY) < deadZone) 0f else (-rawY).coerceIn(-1f, 1f)

        // Always update UI position
        _gamepadJoystickPosition.value = Pair(x, y)

        val isCenter = x == 0f && y == 0f
        val wasCenter = lastStickX == 0f && lastStickY == 0f
        if (isCenter && wasCenter) return

        val now = System.currentTimeMillis()
        if (isCenter || now - lastStickSendTime >= 50L) {
            sendCommand(RobotCommand.Joystick(x, y))
            lastStickSendTime = now
            lastStickX = x
            lastStickY = y
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
        streamingRepository.close()
        repository.close()
    }
}

class RobotControlViewModelFactory(
    private val repository: RobotRepository,
    private val settingsDataStore: SettingsDataStore,
    private val soundManager: SoundManager,
    private val streamingRepository: StreamingRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RobotControlViewModel::class.java)) {
            return RobotControlViewModel(repository, settingsDataStore, soundManager, streamingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
