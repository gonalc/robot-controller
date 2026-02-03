package com.gonzalo.robotcontroller.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gonzalo.robotcontroller.data.preferences.SettingsDataStore
import com.gonzalo.robotcontroller.data.repository.RobotRepository
import com.gonzalo.robotcontroller.domain.model.ConnectionState
import com.gonzalo.robotcontroller.domain.model.RobotCommand
import com.gonzalo.robotcontroller.domain.model.RobotSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RobotControlViewModel(
    private val repository: RobotRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = repository.connectionState

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
        repository.sendCommand(command)
    }

    fun updateServerUrl(url: String) {
        viewModelScope.launch {
            settingsDataStore.updateServerUrl(url)
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
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
