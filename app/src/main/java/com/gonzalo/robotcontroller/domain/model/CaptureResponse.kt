package com.gonzalo.robotcontroller.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CaptureResponse(
    val status: String,
    val command: String,
    val image: String,
    val width: Int,
    val height: Int
)
