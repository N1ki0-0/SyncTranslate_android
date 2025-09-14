package com.example.synctranslate.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.synctranslate.data.local.PreferencesManager
import com.example.synctranslate.data.remote.webrtc.SignalingEvent
import com.example.synctranslate.domain.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class MainAppViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _rtcState = MutableStateFlow(RtcState.IDLE)
    val rtcState = _rtcState.asStateFlow()

    private var signalingJob: Job? = null

    fun startCommunication() {
        if (_rtcState.value != RtcState.IDLE && _rtcState.value != RtcState.CLOSED) return
        _rtcState.value = RtcState.SIGNALING

        val ip = preferencesManager.serverIp ?: return // Нужна проверка
        val port = preferencesManager.serverPort
        val signalingUrl = "ws://$ip:$port/ws" // Пример URL

        signalingJob?.cancel()
        signalingJob = appRepository.getSignalingEvents().onEach { event ->
            when (event) {
                is SignalingEvent.ConnectionEstablished -> {
                    _rtcState.value = RtcState.CREATING
                    appRepository.startRtc()
                }
                is SignalingEvent.AnswerReceived -> {
                    appRepository.onAnswerReceived(event.sdp)
                    // Здесь можно будет перейти в состояние CONNECTED после проверки статуса ICE
                }
                is SignalingEvent.IceCandidateReceived -> {
                    appRepository.onIceCandidateReceived(event.candidate)
                }
                is SignalingEvent.ConnectionClosed -> _rtcState.value = RtcState.CLOSED
                is SignalingEvent.ConnectionError -> _rtcState.value = RtcState.FAILED
            }
        }.launchIn(viewModelScope)

        appRepository.startSignaling(signalingUrl)
    }

    fun stopCommunication() {
        appRepository.stopCommunication()
        _rtcState.value = RtcState.CLOSED
    }

    override fun onCleared() {
        stopCommunication()
        super.onCleared()
    }
}
