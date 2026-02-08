package com.gonzalo.robotcontroller.data.streaming

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.cos
import kotlin.math.sin

class MockFrameGenerator {

    private val width = 640
    private val height = 480
    private val frameIntervalMs = 100L

    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val circlePaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val counterPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
    }

    fun generateFrames(): Flow<Bitmap> = flow {
        var frameCount = 0
        var hue = 0f

        while (true) {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            hue = (hue + 2f) % 360f
            val hsv = floatArrayOf(hue, 0.7f, 0.5f)
            backgroundPaint.color = Color.HSVToColor(hsv)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

            val angle = (frameCount * 5) % 360
            val centerX = width / 2f
            val centerY = height / 2f
            val radius = 100f
            val circleX = centerX + cos(Math.toRadians(angle.toDouble())).toFloat() * radius
            val circleY = centerY + sin(Math.toRadians(angle.toDouble())).toFloat() * radius
            canvas.drawCircle(circleX, circleY, 30f, circlePaint)

            canvas.drawText("TEST MODE", centerX, height * 0.25f, textPaint)

            canvas.drawText("Frame: $frameCount", 20f, height - 20f, counterPaint)

            emit(bitmap)

            frameCount++
            delay(frameIntervalMs)
        }
    }
}
