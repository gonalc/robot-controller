package com.gonzalo.robotcontroller.data.utils

import java.net.URI

object UrlUtils {

    fun extractIpFromWebSocketUrl(wsUrl: String): String? {
        return try {
            val uri = URI(wsUrl)
            uri.host
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun buildStreamUrl(wsUrl: String, streamPort: Int, streamPath: String): String? {
        val ip = extractIpFromWebSocketUrl(wsUrl) ?: return null
        val path = if (streamPath.startsWith("/")) streamPath else "/$streamPath"
        return "http://$ip:$streamPort$path"
    }
}
