package com.gonzalo.robotcontroller.data.streaming

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class MjpegStreamClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    fun connect(streamUrl: String): Flow<Bitmap> = flow {
        val request = Request.Builder()
            .url(streamUrl)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw MjpegException("Failed to connect: ${response.code}")
        }

        val contentType = response.header("Content-Type")
            ?: throw MjpegException("Missing Content-Type header")

        val boundary = extractBoundary(contentType)
            ?: throw MjpegException("Missing boundary in Content-Type: $contentType")

        val boundaryBytes = "--$boundary".toByteArray(Charsets.US_ASCII)
        val body = response.body ?: throw MjpegException("Empty response body")

        val inputStream = BufferedInputStream(body.byteStream(), 65536)
        val buffer = ByteArray(8192)
        val frameBuffer = ByteArrayOutputStream(65536)
        var inFrame = false
        var contentLength = -1

        try {
            while (coroutineContext.isActive) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break

                for (i in 0 until bytesRead) {
                    val b = buffer[i]

                    if (inFrame) {
                        frameBuffer.write(b.toInt())

                        if (contentLength > 0 && frameBuffer.size() >= contentLength) {
                            val frameData = frameBuffer.toByteArray()
                            val bitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.size)
                            if (bitmap != null) {
                                emit(bitmap)
                            }
                            frameBuffer.reset()
                            inFrame = false
                            contentLength = -1
                        }
                    } else {
                        frameBuffer.write(b.toInt())

                        val currentData = frameBuffer.toByteArray()
                        val headerEnd = findHeaderEnd(currentData)

                        if (headerEnd != -1) {
                            val headerStr = String(currentData, 0, headerEnd, Charsets.US_ASCII)
                            contentLength = parseContentLength(headerStr)

                            frameBuffer.reset()
                            val remainingStart = headerEnd + 4
                            if (remainingStart < currentData.size) {
                                frameBuffer.write(currentData, remainingStart, currentData.size - remainingStart)
                            }
                            inFrame = true
                        }

                        if (frameBuffer.size() > 1024 && !inFrame) {
                            val data = frameBuffer.toByteArray()
                            val boundaryIndex = findBoundary(data, boundaryBytes)
                            if (boundaryIndex != -1) {
                                frameBuffer.reset()
                                val afterBoundary = boundaryIndex + boundaryBytes.size
                                if (afterBoundary < data.size) {
                                    val skipNewlines = skipNewlines(data, afterBoundary)
                                    if (skipNewlines < data.size) {
                                        frameBuffer.write(data, skipNewlines, data.size - skipNewlines)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            inputStream.close()
            response.close()
        }
    }.flowOn(Dispatchers.IO)

    private fun extractBoundary(contentType: String): String? {
        val parts = contentType.split(";").map { it.trim() }
        for (part in parts) {
            if (part.startsWith("boundary=")) {
                return part.substringAfter("boundary=").trim('"')
            }
        }
        return null
    }

    private fun findHeaderEnd(data: ByteArray): Int {
        for (i in 0 until data.size - 3) {
            if (data[i] == '\r'.code.toByte() &&
                data[i + 1] == '\n'.code.toByte() &&
                data[i + 2] == '\r'.code.toByte() &&
                data[i + 3] == '\n'.code.toByte()
            ) {
                return i
            }
        }
        return -1
    }

    private fun parseContentLength(header: String): Int {
        val lines = header.split("\r\n", "\n")
        for (line in lines) {
            if (line.lowercase().startsWith("content-length:")) {
                return line.substringAfter(":").trim().toIntOrNull() ?: -1
            }
        }
        return -1
    }

    private fun findBoundary(data: ByteArray, boundary: ByteArray): Int {
        outer@ for (i in 0 until data.size - boundary.size) {
            for (j in boundary.indices) {
                if (data[i + j] != boundary[j]) continue@outer
            }
            return i
        }
        return -1
    }

    private fun skipNewlines(data: ByteArray, start: Int): Int {
        var i = start
        while (i < data.size && (data[i] == '\r'.code.toByte() || data[i] == '\n'.code.toByte())) {
            i++
        }
        return i
    }

    fun close() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}

class MjpegException(message: String) : Exception(message)
