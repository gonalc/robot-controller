package com.gonzalo.robotcontroller.data.repository

import android.graphics.Bitmap
import com.gonzalo.robotcontroller.data.streaming.MjpegStreamClient
import com.gonzalo.robotcontroller.data.streaming.MockFrameGenerator
import com.gonzalo.robotcontroller.data.utils.UrlUtils
import com.gonzalo.robotcontroller.domain.model.StreamingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class StreamingRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val mjpegClient = MjpegStreamClient()
    private val mockGenerator = MockFrameGenerator()

    private val _streamingState = MutableStateFlow<StreamingState>(StreamingState.Idle)
    val streamingState: StateFlow<StreamingState> = _streamingState.asStateFlow()

    private val _currentFrame = MutableStateFlow<Bitmap?>(null)
    val currentFrame: StateFlow<Bitmap?> = _currentFrame.asStateFlow()

    private var streamJob: Job? = null

    fun startStream(wsUrl: String, streamPort: Int, streamPath: String) {
        val streamUrl = UrlUtils.buildStreamUrl(wsUrl, streamPort, streamPath)
        if (streamUrl == null) {
            _streamingState.value = StreamingState.Error("Invalid WebSocket URL")
            return
        }

        stopStream()

        streamJob = scope.launch {
            mjpegClient.connect(streamUrl)
                .onStart {
                    _streamingState.value = StreamingState.Connecting
                }
                .catch { e ->
                    _streamingState.value = StreamingState.Error(e.message ?: "Stream error")
                }
                .onCompletion { cause ->
                    if (cause == null && _streamingState.value is StreamingState.Streaming) {
                        _streamingState.value = StreamingState.Idle
                    }
                }
                .collect { frame ->
                    if (_streamingState.value !is StreamingState.Streaming) {
                        _streamingState.value = StreamingState.Streaming
                    }
                    _currentFrame.value?.recycle()
                    _currentFrame.value = frame
                }
        }
    }

    fun startMockStream() {
        stopStream()

        streamJob = scope.launch {
            mockGenerator.generateFrames()
                .onStart {
                    _streamingState.value = StreamingState.Streaming
                }
                .catch { e ->
                    _streamingState.value = StreamingState.Error(e.message ?: "Mock stream error")
                }
                .collect { frame ->
                    _currentFrame.value?.recycle()
                    _currentFrame.value = frame
                }
        }
    }

    fun stopStream() {
        streamJob?.cancel()
        streamJob = null
        _currentFrame.value?.recycle()
        _currentFrame.value = null
        _streamingState.value = StreamingState.Idle
    }

    fun close() {
        stopStream()
        mjpegClient.close()
        scope.cancel()
    }
}
