package com.example.synctranslate.data.repository

import com.example.synctranslate.data.remote.http.ApiService
import com.example.synctranslate.data.remote.UdpClient
import com.example.synctranslate.domain.repository.AudioRepository
import com.example.synctranslate.util.AppLogger
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepositoryImpl @Inject constructor(
    private val udpClient: UdpClient,
    private val apiService: ApiService,
) : AudioRepository {

    override fun getIncomingAudioStream(): Flow<ByteArray> {
        return udpClient.incomingPackets.map { it.data }
    }

    override fun startUdpListening(port: Int) {
        udpClient.startListening(port)
    }

    override fun stopUdpListening() {
        udpClient.stopListening()
    }

    override suspend fun sendAudioChunk(audioChunk: ByteArray, ip: String, port: Int) {
        udpClient.send(audioChunk, ip, port)
    }

    override suspend fun uploadSetupAudio(audioFile: File): Boolean {
        return try {
            val requestFile = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)

            val response = apiService.uploadSetupAudio(body)
            if (!response.isSuccessful) {
                AppLogger.e("AudioRepository", "Server error on upload: ${response.errorBody()?.string()}")
            }
            response.isSuccessful
        } catch (e: Exception) {
            AppLogger.e("AudioRepository", "Error uploading setup audio $e")
            false
        }
    }
}