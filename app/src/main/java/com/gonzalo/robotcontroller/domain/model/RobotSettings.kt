package com.gonzalo.robotcontroller.domain.model

data class RobotSettings(
    val serverUrl: String = "ws://192.168.1.100:8765",
    val reconnectEnabled: Boolean = true,
    val maxReconnectAttempts: Int = 5
)
