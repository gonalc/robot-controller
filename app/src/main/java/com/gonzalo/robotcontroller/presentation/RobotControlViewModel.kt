package com.gonzalo.robotcontroller.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gonzalo.robotcontroller.data.preferences.SettingsDataStore
import com.gonzalo.robotcontroller.data.repository.RobotRepository
import com.gonzalo.robotcontroller.domain.model.ConnectionState
import com.gonzalo.robotcontroller.domain.model.RobotCommand
import com.gonzalo.robotcontroller.domain.model.RobotSettings
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RobotControlViewModel(
    private val repository: RobotRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    private val _testMode = MutableStateFlow(false)
    val testMode: StateFlow<Boolean> = _testMode.asStateFlow()

    // Gamepad joystick position for UI display
    private val _gamepadJoystickPosition = MutableStateFlow(Pair(0f, 0f))
    val gamepadJoystickPosition: StateFlow<Pair<Float, Float>> = _gamepadJoystickPosition.asStateFlow()

    // Speed control
    private val _speed = MutableStateFlow(50)
    val speed: StateFlow<Int> = _speed.asStateFlow()

    fun setSpeed(value: Int) {
        val newSpeed = value.coerceIn(0, 100)
        if (_speed.value != newSpeed) {
            _speed.value = newSpeed
            sendCommand(RobotCommand.Speed(newSpeed))
        }
    }

    fun adjustSpeed(delta: Int) {
        setSpeed(_speed.value + delta)
    }

    fun toggleTestMode() {
        _testMode.value = !_testMode.value
    }

    val settings: StateFlow<RobotSettings> = settingsDataStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RobotSettings()
    )

    init {
        viewModelScope.launch {
            settings.collect { newSettings ->
                repository.updateSettings(newSettings)
            }
        }
    }

    fun connect() {
        repository.connect(settings.value.serverUrl)
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun sendCommand(command: RobotCommand) {
        if (_testMode.value) return
        repository.sendCommand(command)
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
        repository.close()
    }
}

class RobotControlViewModelFactory(
    private val repository: RobotRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RobotControlViewModel::class.java)) {
            return RobotControlViewModel(repository, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
