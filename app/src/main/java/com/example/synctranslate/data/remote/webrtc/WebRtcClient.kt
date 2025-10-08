package com.example.synctranslate.data.remote.webrtc

import android.content.Context
import com.example.synctranslate.util.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.nio.ByteBuffer
import kotlin.text.Charsets
import kotlin.coroutines.resume

data class AudioPacket(val type: String = "audio", val data: String)
data class IncomingPacket(val type: String?, val data: String?)

@Singleton
class WebRtcClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        // 1. Сначала нужно инициализировать WebRTC в контексте нашего приложения
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        // 2. Создаем и настраиваем аудио-модуль.
        // Здесь мы включаем встроенные в Android улучшайзеры звука.
        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true) // Аппаратное эхоподавление
            .setUseHardwareNoiseSuppressor(true)      // Аппаратное шумоподавление
            .createAudioDeviceModule()

        // 3. Собираем саму фабрику с нашими аудио-настройками
        PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()
            .also {
                AppLogger.i("WebRtcClient", "PeerConnectionFactory создана")
            }
    }
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    // Поток для входящих аудиоданных от сервера
    private val _incomingAudioDataFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val incomingAudioDataFlow = _incomingAudioDataFlow.asSharedFlow()

    private val _dataChannelReadyFlow = MutableStateFlow(false)
    val dataChannelReadyFlow = _dataChannelReadyFlow.asStateFlow()

    @Volatile
    private var isReadyForStreaming = false

    private val audioStartPayload = mapOf(
        "type" to "start",
        "samplerate" to 48000,
        "channels" to 1,
        "dtype" to "int16",
        "frames_per_chunk" to 1024
    )

    private val dataChannelObserver = object : DataChannel.Observer {
        override fun onBufferedAmountChange(previousAmount: Long) {}
        override fun onStateChange() {
            val state = dataChannel?.state()
            AppLogger.i("AudioFlow", "3. WebRTC: Состояние DataChannel изменилось на: $state")
            if (state == DataChannel.State.OPEN) {
                isReadyForStreaming = false
                _dataChannelReadyFlow.value = false
                sendStartMessage()
            } else if (state == DataChannel.State.CLOSING || state == DataChannel.State.CLOSED) {
                isReadyForStreaming = false
                _dataChannelReadyFlow.value = false
            }
        }
        override fun onMessage(buffer: DataChannel.Buffer) {
            if (buffer.binary) {
                val data = ByteArray(buffer.data.remaining())
                buffer.data.get(data)
                if (data.isNotEmpty()) {
                    _incomingAudioDataFlow.tryEmit(data)
                }
            } else {
                val bytes = ByteArray(buffer.data.remaining())
                buffer.data.get(bytes)
                val message = bytes.toString(Charsets.UTF_8)
                handleControlMessage(message)
            }
        }
    }

    private fun sendStartMessage() {
        val message = gson.toJson(audioStartPayload)
        val buffer = DataChannel.Buffer(ByteBuffer.wrap(message.toByteArray(Charsets.UTF_8)), false)
        dataChannel?.send(buffer)
        AppLogger.i("AudioFlow", "3. WebRTC: Отправил параметры аудио на сервер")
    }

    private fun handleControlMessage(message: String) {
        runCatching {
            val packet = gson.fromJson(message, IncomingPacket::class.java)
            when (packet?.type) {
                "ack_start" -> {
                    isReadyForStreaming = true
                    _dataChannelReadyFlow.value = true
                    AppLogger.i("AudioFlow", "3. WebRTC: Получен ack от сервера, можно стримить")
                }
                else -> AppLogger.d("AudioFlow", "3. WebRTC: Неизвестное сообщение канала: $message")
            }
        }.onFailure {
            AppLogger.e("AudioFlow", "3. WebRTC: Ошибка обработки control-сообщения", it)
        }
    }

    suspend fun createOffer(peerConnectionObserver: PeerConnection.Observer): SessionDescription? = suspendCancellableCoroutine { continuation ->
        val iceServers = listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
        peerConnection = peerConnectionFactory.createPeerConnection(iceServers, peerConnectionObserver)

        val dcInit = DataChannel.Init()
        dataChannel = peerConnection?.createDataChannel("filetransfer", dcInit)
        dataChannel?.registerObserver(dataChannelObserver)

        continuation.invokeOnCancellation {
            peerConnection?.close()
            peerConnection = null
        }

        peerConnection?.createOffer(object : SdpObserverImpl("createOffer") {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (continuation.isActive && sdp != null) {
                    peerConnection?.setLocalDescription(SdpObserverImpl("setLocalDescription"), sdp)
                    continuation.resume(sdp)
                } else if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }, MediaConstraints())
    }


    // Наблюдатель за PeerConnection остается почти таким же (без onAddTrack)
    private val peerConnectionObserver = object : PeerConnection.Observer {
        // Вызывается при изменении состояния ICE-соединения (это статус P2P-подключения).
        // Самые важные состояния: CONNECTED, FAILED, DISCONNECTED.
        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
            AppLogger.i("WebRtcClient", "Статус ICE соединения: $newState")
            // Здесь можно будет обновлять UI, если соединение разорвалось
        }

        // Вызывается, когда WebRTC находит локальный сетевой адрес (кандидата),
        // который можно использовать для подключения. В нашей HTTP-архитектуре
        // мы не пересылаем их по отдельности, так как они включаются в initial Offer.
        override fun onIceCandidate(candidate: IceCandidate?) {
            AppLogger.d("WebRtcClient", "Найден локальный ICE кандидат: $candidate")
            // В WebSocket-сигналинге мы бы здесь отправляли кандидата на сервер.
            // В HTTP-сигналинге этот колбэк менее важен.
        }

        // Вызывается, когда удаленная сторона (сервер) создает DataChannel.
        // В нашем случае мы сами создаем канал, поэтому этот метод не вызовется.
        override fun onDataChannel(dataChannel: DataChannel?) {
            AppLogger.i("WebRtcClient", "Получен удаленный DataChannel: ${dataChannel?.label()}")
        }

        // Вызывается при изменении состояния сигналинга (обмена Offer/Answer)
        override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
            AppLogger.d("WebRtcClient", "Статус сигналинга: $newState")
        }

        // Этот колбэк больше не используется, так как мы работаем через DataChannel, а не AudioTrack
        override fun onAddTrack(rtpReceiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}

        // Остальные колбэки для полноты картины и отладки
        override fun onIceConnectionReceivingChange(p0: Boolean) {}
        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
        override fun onAddStream(p0: MediaStream?) {}
        override fun onRemoveStream(p0: MediaStream?) {}
        override fun onRenegotiationNeeded() {}
    }

    fun sendAudioData(data: ByteArray) {
        // --- ШАГ 4: ФИНАЛЬНАЯ ПРОВЕРКА ПЕРЕД ОТПРАВКОЙ ---
        if (!isReadyForStreaming) {
            AppLogger.w("AudioFlow", "4. WebRTC: !!! Канал ещё не готов, дропаю ${data.size} байт")
            return
        }

        if (dataChannel?.state() == DataChannel.State.OPEN) {
            AppLogger.i("AudioFlow", "4. WebRTC: >>> DataChannel ОТКРЫТ. Отправляю ${data.size} байт.")
            val byteBuffer = ByteBuffer.wrap(data)
            val buffer = DataChannel.Buffer(byteBuffer, true)
            dataChannel?.send(buffer)
        } else {
            // Этот лог покажет, если мы пытаемся отправить данные до того, как канал открылся
            AppLogger.w("AudioFlow", "4. WebRTC: !!! ПОПЫТКА ОТПРАВКИ, НО DataChannel НЕ ОТКРЫТ. Текущее состояние: ${dataChannel?.state()} !!!")
        }
    }

    fun onAnswerReceived(answer: SessionDescription) {
        peerConnection?.setRemoteDescription(SdpObserverImpl("setRemoteDescription"), answer)
    }

    fun stop() {
        dataChannel?.unregisterObserver()
        dataChannel?.close()
        peerConnection?.close()
        dataChannel = null
        peerConnection = null
        isReadyForStreaming = false
        _dataChannelReadyFlow.value = false
    }
}




//    // Начинаем "звонок"
//    fun call() {
//        val iceServers = listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
//        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
//        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, peerConnectionObserver)
//        peerConnection?.addTrack(localAudioTrack)
//
//        val constraints = MediaConstraints()
//        peerConnection?.createOffer(object : SdpObserverImpl("createOffer") {
//            override fun onCreateSuccess(sdp: SessionDescription?) {
//                super.onCreateSuccess(sdp)
//                peerConnection?.setLocalDescription(SdpObserverImpl("setLocalDescription"))
//                sdp?.let { signalingClient.send(SignalingCommand.Offer(it.description)) }
//            }
//        }, constraints)
//    }
//
//    // Получаем ответ от сервера
//    fun onAnswerReceived(sdp: String) {
//        val remoteSdp = SessionDescription(SessionDescription.Type.ANSWER, sdp)
//        peerConnection?.setRemoteDescription(SdpObserverImpl("setRemoteDescription"), remoteSdp)
//    }
//
//    // Добавляем ICE-кандидата от сервера
//    fun addIceCandidate(payload: IceCandidatePayload) {
//        val candidate = IceCandidate(payload.sdpMid, payload.sdpMLineIndex, payload.sdp)
//        peerConnection?.addIceCandidate(candidate)
//    }
//
//    fun stop() {
//        peerConnection?.close()
//        peerConnection = null
//    }
