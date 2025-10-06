package com.example.synctranslate.data.repository

import com.example.synctranslate.data.remote.http.ApiService
import com.example.synctranslate.data.remote.http.SdpPayload
import com.example.synctranslate.data.remote.webrtc.IceCandidatePayload
import com.example.synctranslate.data.remote.webrtc.SignalingClient
import com.example.synctranslate.data.remote.webrtc.SignalingEvent
import com.example.synctranslate.data.remote.webrtc.WebRtcClient
import com.example.synctranslate.domain.repository.AppRepository
import com.example.synctranslate.util.AppLogger
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.webrtc.SessionDescription
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val signalingClient: SignalingClient,
    private val webRtcClient: WebRtcClient
) : AppRepository {

    override suspend fun uploadSetupAudio(audioFile: File): Boolean {
        return try {
            val requestFile = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            val response = apiService.uploadSetupAudio(body)
            response.isSuccessful
        } catch (e: Exception) {
            AppLogger.e("AppRepository", "Ошибка загрузки файла $e", )
            false
        }
    }
//
//    override fun startSignaling(url: String) {
//        signalingClient.connect(url)
//    }
//
//    override fun getSignalingEvents(): Flow<SignalingEvent> {
//        return signalingClient.eventFlow
//    }
//
//    override fun startRtc() {
//        webRtcClient.call()
//    }
//
//    override fun onAnswerReceived(sdp: String) {
//        webRtcClient.onAnswerReceived(sdp)
//    }
//
//    override fun onIceCandidateReceived(payload: IceCandidatePayload) {
//        webRtcClient.addIceCandidate(payload)
//    }
//
//    override fun stopCommunication() {
//        signalingClient.disconnect()
//        webRtcClient.stop()
//    }

    override suspend fun startRtcSession(offer: SessionDescription): SessionDescription? {
        return try {
            val offerPayload = SdpPayload(sdp = offer.description, type = offer.type.canonicalForm())
            val response = apiService.sendOffer(offerPayload)
            if (response.isSuccessful && response.body() != null) {
                val answerPayload = response.body()!!
                SessionDescription(SessionDescription.Type.fromCanonicalForm(answerPayload.type), answerPayload.sdp)
            } else {
                AppLogger.e("AppRepository", "Ошибка получения Answer от сервера: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            AppLogger.e("AppRepository", "Исключение при обмене SDP $e", )
            null
        }
    }

    override fun stopRtcSession() {
        webRtcClient.stop()
    }
}