package com.gonzalo.robotcontroller.domain.model

import kotlinx.serialization.Serializable
import java.util.Locale

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

    @Serializable
    data class Joystick(val x: Float, val y: Float) : RobotCommand()

    @Serializable
    data class Capture(val width: Int, val height: Int) : RobotCommand()

    fun toJson(): String {
        return when (this) {
            is Forward -> """{"command":"forward"}"""
            is Backward -> """{"command":"backward"}"""
            is Left -> """{"command":"left"}"""
            is Right -> """{"command":"right"}"""
            is Stop -> """{"command":"stop"}"""
            is Speed -> """{"command":"speed","value":$value}"""
            is Joystick -> """{"command":"joystick","x":${String.format(Locale.ROOT, "%.2f", x)},"y":${String.format(Locale.ROOT, "%.2f", y)}}"""
            is Capture -> """{"command":"capture","width":$width,"height":$height}"""
        }
    }
}
