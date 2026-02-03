package com.gonzalo.robotcontroller.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed class RobotCommand {
    @Serializable
    data object Forward : RobotCommand()

    @Serializable
    data object Backward : RobotCommand()

    @Serializable
    data object Left : RobotCommand()

    @Serializable
    data object Right : RobotCommand()

    @Serializable
    data object Stop : RobotCommand()

    @Serializable
    data class Speed(val value: Int) : RobotCommand()

    fun toJson(): String {
        return when (this) {
            is Forward -> """{"command":"right"}"""
            is Backward -> """{"command":"left"}"""
            is Left -> """{"command":"forward"}"""
            is Right -> """{"command":"backward"}"""
            is Stop -> """{"command":"stop"}"""
            is Speed -> """{"command":"speed","value":$value}"""
        }
    }
}
