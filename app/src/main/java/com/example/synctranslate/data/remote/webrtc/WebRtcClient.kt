package com.example.synctranslate.data.remote.webrtc

import android.content.Context
import com.example.synctranslate.util.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule


@Singleton
class WebRtcClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signalingClient: SignalingClient
) {
    // Это сердце всей WebRTC-логики
    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )
        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()
        PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()
    }

    private val audioSource: AudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    private val localAudioTrack: AudioTrack by lazy { peerConnectionFactory.createAudioTrack("local_audio", audioSource) }
    private var peerConnection: PeerConnection? = null

    // Наблюдатель за состоянием PeerConnection
    private val peerConnectionObserver = object : PeerConnection.Observer {
        override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
            AppLogger.d("WebRtcClient", "SignalingState: $newState")
        }
        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
            AppLogger.d("WebRtcClient", "IceConnectionState: $newState")
        }
        override fun onIceCandidate(candidate: IceCandidate?) {
            candidate?.let {
                val payload = IceCandidatePayload(it.sdp, it.sdpMid, it.sdpMLineIndex)
                signalingClient.send(SignalingCommand.IceCandidate(payload))
            }
        }
        override fun onAddTrack(rtpReceiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
            AppLogger.i("WebRtcClient", ">>> Получен удаленный аудиопоток <<<")
        }
        // Остальные методы можно оставить пустыми
        override fun onIceConnectionReceivingChange(p0: Boolean) {}
        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
        override fun onAddStream(p0: MediaStream?) {}
        override fun onRemoveStream(p0: MediaStream?) {}
        override fun onDataChannel(p0: DataChannel?) {}
        override fun onRenegotiationNeeded() {}
    }

    // Начинаем "звонок"
    fun call() {
        val iceServers = listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, peerConnectionObserver)
        peerConnection?.addTrack(localAudioTrack)

        val constraints = MediaConstraints()
        peerConnection?.createOffer(object : SdpObserverImpl("createOffer") {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                super.onCreateSuccess(sdp)
                peerConnection?.setLocalDescription(SdpObserverImpl("setLocalDescription"))
                sdp?.let { signalingClient.send(SignalingCommand.Offer(it.description)) }
            }
        }, constraints)
    }

    // Получаем ответ от сервера
    fun onAnswerReceived(sdp: String) {
        val remoteSdp = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        peerConnection?.setRemoteDescription(SdpObserverImpl("setRemoteDescription"), remoteSdp)
    }

    // Добавляем ICE-кандидата от сервера
    fun addIceCandidate(payload: IceCandidatePayload) {
        val candidate = IceCandidate(payload.sdpMid, payload.sdpMLineIndex, payload.sdp)
        peerConnection?.addIceCandidate(candidate)
    }

    fun stop() {
        peerConnection?.close()
        peerConnection = null
    }
}