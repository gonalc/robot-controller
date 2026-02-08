package com.gonzalo.robotcontroller.data.repository

import com.gonzalo.robotcontroller.data.websocket.WebSocketClient
import com.gonzalo.robotcontroller.data.websocket.WebSocketEvent
import com.gonzalo.robotcontroller.domain.model.CaptureResponse
import com.gonzalo.robotcontroller.domain.model.ConnectionState
import com.gonzalo.robotcontroller.domain.model.RobotCommand
import com.gonzalo.robotcontroller.domain.model.RobotSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RobotRepository(
    private val webSocketClient: WebSocketClient
) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _captureResponse = Channel<CaptureResponse>(Channel.BUFFERED)
    val captureResponse = _captureResponse.receiveAsFlow()

    private val json = Json { ignoreUnknownKeys = true }

    private var connectionJob: Job? = null
    private var reconnectAttempts = 0
    private var settings = RobotSettings()

    fun connect(url: String = settings.serverUrl) {
        if (_connectionState.value is ConnectionState.Connected ||
            _connectionState.value is ConnectionState.Connecting) {
            return
        }

        _connectionState.value = ConnectionState.Connecting
        reconnectAttempts = 0

        connectionJob?.cancel()
        connectionJob = coroutineScope.launch {
            webSocketClient.connect(url).collect { event ->
                handleWebSocketEvent(event, url)
            }
        }
    }

    fun disconnect() {
        reconnectAttempts = settings.maxReconnectAttempts
        connectionJob?.cancel()
        webSocketClient.disconnect()
        _connectionState.value = ConnectionState.Disconnected
    }

    fun sendCommand(command: RobotCommand) {
        if (_connectionState.value is ConnectionState.Connected) {
            webSocketClient.send(command.toJson())
        }
    }

    private suspend fun handleWebSocketEvent(event: WebSocketEvent, url: String) {
        when (event) {
            is WebSocketEvent.Connected -> {
                _connectionState.value = ConnectionState.Connected
                reconnectAttempts = 0
            }
            is WebSocketEvent.Disconnected -> {
                _connectionState.value = ConnectionState.Disconnected
                attemptReconnect(url)
            }
            is WebSocketEvent.Error -> {
                _connectionState.value = ConnectionState.Error(event.message)
                attemptReconnect(url)
            }
            is WebSocketEvent.MessageReceived -> {
                parseMessage(event.message)
            }
        }
    }

    private suspend fun attemptReconnect(url: String) {
        if (!settings.reconnectEnabled || reconnectAttempts >= settings.maxReconnectAttempts) {
            return
        }

        reconnectAttempts++
        val delayMs = minOf(1000L * (1 shl (reconnectAttempts - 1)), 30000L)

        _connectionState.value = ConnectionState.Error("Reconnecting in ${delayMs / 1000}s (attempt $reconnectAttempts/${settings.maxReconnectAttempts})")
        delay(delayMs)

        if (_connectionState.value !is ConnectionState.Connected) {
            connect(url)
        }
    }

    private fun parseMessage(message: String) {
        try {
            val response = json.decodeFromString<CaptureResponse>(message)
            if (response.command == "capture" && response.status == "ok") {
                _captureResponse.trySend(response)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateSettings(newSettings: RobotSettings) {
        settings = newSettings
    }

    fun close() {
        disconnect()
        coroutineScope.cancel()
    }
}
