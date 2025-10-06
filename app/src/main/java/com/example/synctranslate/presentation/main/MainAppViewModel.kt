package com.example.synctranslate.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.synctranslate.data.local.PreferencesManager
import com.example.synctranslate.data.remote.webrtc.SignalingEvent
import com.example.synctranslate.data.remote.webrtc.WebRtcClient
import com.example.synctranslate.domain.repository.AppRepository
import com.example.synctranslate.util.AppLogger
import com.example.synctranslate.util.AudioPlayerUtil
import com.example.synctranslate.util.AudioRecorderUtil
import com.example.synctranslate.util.AudioSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import javax.inject.Inject

@HiltViewModel
class MainAppViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val webRtcClient: WebRtcClient,
    private val audioRecorderUtil: AudioRecorderUtil,
    private val audioPlayerUtil: AudioPlayerUtil
) : ViewModel() {

    private val _rtcState = MutableStateFlow(RtcState.IDLE)
    val rtcState = _rtcState.asStateFlow()

    private var audioSenderJob: Job? = null
    private var audioReceiverJob: Job? = null

    fun hasAudioPermission(): Boolean {
        return audioRecorderUtil.hasRecordAudioPermission()
    }

    private val peerConnectionObserver = object : PeerConnection.Observer {
        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
            AppLogger.i("MainViewModel", "Статус ICE соединения: $newState")
            // Обновляем UI в зависимости от статуса
            when (newState) {
                PeerConnection.IceConnectionState.CONNECTED -> _rtcState.value = RtcState.CONNECTED
                PeerConnection.IceConnectionState.FAILED -> _rtcState.value = RtcState.FAILED
                PeerConnection.IceConnectionState.DISCONNECTED,
                PeerConnection.IceConnectionState.CLOSED -> _rtcState.value = RtcState.CLOSED
                else -> {}
            }
        }

        // Оставляем остальные методы пустыми, так как основная логика в WebRtcClient
        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
        override fun onIceConnectionReceivingChange(p0: Boolean) {}
        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
        override fun onIceCandidate(p0: IceCandidate?) {}
        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
        override fun onAddStream(p0: MediaStream?) {}
        override fun onRemoveStream(p0: MediaStream?) {}
        override fun onDataChannel(p0: org.webrtc.DataChannel?) {}
        override fun onRenegotiationNeeded() {}
        override fun onAddTrack(p0: org.webrtc.RtpReceiver?, p1: Array<out MediaStream>?) {}
    }

    fun startCommunication() {
        if (_rtcState.value != RtcState.IDLE && _rtcState.value != RtcState.CLOSED) return

        viewModelScope.launch {
            _rtcState.value = RtcState.CREATING

            // 1. Создаем Offer
            val offer = webRtcClient.createOffer(peerConnectionObserver)
            if (offer == null) {
                _rtcState.value = RtcState.FAILED
                return@launch
            }

            // 2. Отправляем Offer на сервер и ждем Answer
            val answer = appRepository.startRtcSession(offer)
            if (answer == null) {
                _rtcState.value = RtcState.FAILED
                webRtcClient.stop()
                return@launch
            }

            // 3. Устанавливаем полученный Answer
            webRtcClient.onAnswerReceived(answer)
            _rtcState.value = RtcState.CONNECTED

            // Включаем громкую связь по умолчанию для удобства
            startAudioFlow()
        }
    }

    private fun startAudioFlow() {
        // 1. Готовим плеер
        audioPlayerUtil.prepareToPlay()

        // 2. Начинаем слушать входящие данные от WebRtcClient и играть их
        audioReceiverJob = webRtcClient.incomingAudioDataFlow
            .onEach { audioData ->
                AppLogger.i("AudioFlow", "5. Плеер: Получен ответ от сервера ${audioData.size} байт")
                audioPlayerUtil.playAudioChunk(audioData) }
            .launchIn(viewModelScope)

        // 3. Начинаем запись с микрофона
        audioRecorderUtil.startStreamingAudio(viewModelScope)

        // 4. Начинаем слушать микрофон и отправлять данные в WebRtcClient
        audioSenderJob = audioRecorderUtil.getAudioStream()
            .onEach { audioData ->
                AppLogger.d("AudioFlow", "2. ViewModel: Получен чанк для отправки ${audioData.size} байт")
                webRtcClient.sendAudioData(audioData)
                AppLogger.d("AudioFlow", "Sidetone: Воспроизвожу локально ${audioData.size} байт")
                audioPlayerUtil.playAudioChunk(audioData)
            }
            .launchIn(viewModelScope)
    }

    fun stopCommunication() {
        audioSenderJob?.cancel()
        audioReceiverJob?.cancel()
        audioRecorderUtil.stopStreamingAudio()
        audioPlayerUtil.stopAndRelease()
        appRepository.stopRtcSession()
        _rtcState.value = RtcState.CLOSED
    }

    override fun onCleared() {
        stopCommunication()
        super.onCleared()
    }
}
