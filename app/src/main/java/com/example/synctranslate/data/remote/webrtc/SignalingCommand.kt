package com.example.synctranslate.data.remote.webrtc

import com.google.gson.annotations.SerializedName

// Модели для JSON-сообщений, которыми мы обмениваемся с сервером
sealed class SignalingCommand {
    data class Offer(val sdp: String, val type: String = "offer") : SignalingCommand()
    data class Answer(val sdp: String, val type: String = "answer") : SignalingCommand()
    data class IceCandidate(
        val candidate: IceCandidatePayload,
        val type: String = "ice_candidate"
    ) : SignalingCommand()
}

data class IceCandidatePayload(
    @SerializedName("candidate") val sdp: String,
    @SerializedName("sdpMid") val sdpMid: String,
    @SerializedName("sdpMLineIndex") val sdpMLineIndex: Int
)

// События от сигнального клиента для ViewModel
sealed class SignalingEvent {
    object ConnectionEstablished : SignalingEvent()
    data class AnswerReceived(val sdp: String) : SignalingEvent()
    data class IceCandidateReceived(val candidate: IceCandidatePayload) : SignalingEvent()
    object ConnectionClosed : SignalingEvent()
    data class ConnectionError(val description: String) : SignalingEvent()
}