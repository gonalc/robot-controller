package com.gonzalo.robotcontroller.data.websocket

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketClient(private val client: OkHttpClient = OkHttpClient()) {

    private var webSocket: WebSocket? = null

    fun connect(url: String): Flow<WebSocketEvent> = callbackFlow {
        val request = Request.Builder().url(url).build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                trySend(WebSocketEvent.Connected)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                trySend(WebSocketEvent.MessageReceived(text))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                trySend(WebSocketEvent.Error(t.message ?: "Unknown error"))
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                trySend(WebSocketEvent.Disconnected)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                trySend(WebSocketEvent.Disconnected)
            }
        }

        webSocket = client.newWebSocket(request, listener)

        awaitClose {
            webSocket?.close(1000, "Client closed")
            webSocket = null
        }
    }

    fun send(message: String): Boolean {
        return webSocket?.send(message) ?: false
    }

    fun disconnect() {
        webSocket?.close(1000, "Manual disconnect")
        webSocket = null
    }
}

sealed class WebSocketEvent {
    data object Connected : WebSocketEvent()
    data object Disconnected : WebSocketEvent()
    data class MessageReceived(val message: String) : WebSocketEvent()
    data class Error(val message: String) : WebSocketEvent()
}
