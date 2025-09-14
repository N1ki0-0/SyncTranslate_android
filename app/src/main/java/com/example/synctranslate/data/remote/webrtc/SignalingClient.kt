package com.example.synctranslate.data.remote.webrtc

import com.example.synctranslate.util.AppLogger
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalingClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    // Этот класс управляет WebSocket-соединением для сигналинга
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null

    private val _eventFlow = MutableSharedFlow<SignalingEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun connect(url: String) {
        if (webSocket != null) return
        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                scope.launch { _eventFlow.emit(SignalingEvent.ConnectionEstablished) }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val jsonObject = gson.fromJson(text, Map::class.java)
                    when (jsonObject["type"]) {
                        "answer" -> {
                            val sdp = jsonObject["sdp"] as String
                            scope.launch { _eventFlow.emit(SignalingEvent.AnswerReceived(sdp)) }
                        }
                        "ice_candidate" -> {
                            val candidatePayload = gson.fromJson(text, SignalingCommand.IceCandidate::class.java).candidate
                            scope.launch { _eventFlow.emit(SignalingEvent.IceCandidateReceived(candidatePayload)) }
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e("SignalingClient", "Ошибка парсинга JSON $e",)
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                scope.launch { _eventFlow.emit(SignalingEvent.ConnectionClosed) }
                webSocket = null
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                scope.launch { _eventFlow.emit(SignalingEvent.ConnectionError(t.message ?: "Unknown error")) }
                webSocket = null
            }
        })
    }

    fun send(command: SignalingCommand) {
        val jsonString = gson.toJson(command)
        webSocket?.send(jsonString)
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnected.")
    }
}