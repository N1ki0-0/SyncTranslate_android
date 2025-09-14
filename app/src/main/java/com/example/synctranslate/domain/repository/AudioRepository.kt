package com.example.synctranslate.domain.repository

import com.example.synctranslate.data.local.SeqUdpPacket
import kotlinx.coroutines.flow.Flow
import okio.ByteString
import java.io.File

interface AudioRepository {
    fun getIncomingAudioStream(): Flow<ByteArray>
    fun startUdpListening(port: Int)
    fun stopUdpListening()
    suspend fun sendAudioChunk(audioChunk: ByteArray, ip: String, port: Int)
    suspend fun uploadSetupAudio(audioFile: File): Boolean

}