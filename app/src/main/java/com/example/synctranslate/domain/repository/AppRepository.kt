package com.example.synctranslate.domain.repository

import com.example.synctranslate.data.remote.webrtc.IceCandidatePayload
import com.example.synctranslate.data.remote.webrtc.SignalingEvent
import kotlinx.coroutines.flow.Flow
import org.webrtc.SessionDescription
import java.io.File

interface AppRepository {
    suspend fun uploadSetupAudio(audioFile: File): Boolean

    // WebRTC & Signaling
//    fun startSignaling(url: String)
//    fun getSignalingEvents(): Flow<SignalingEvent>
//    fun startRtc()
//    fun onAnswerReceived(sdp: String)
//    fun onIceCandidateReceived(payload: IceCandidatePayload)
//    fun stopCommunication()

    suspend fun startRtcSession(offer: SessionDescription): SessionDescription?

    fun stopRtcSession()
}