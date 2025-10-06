package com.example.synctranslate.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerUtil @Inject constructor() {
    private var audioTrack: AudioTrack? = null

    private val sampleRate = 48000 // Используем 48000 Гц, как в Python-скрипте
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun prepareToPlay() {
        if (audioTrack != null) return
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audioTrack?.play()
    }

    fun playAudioChunk(data: ByteArray) {
        audioTrack?.write(data, 0, data.size)
    }

    fun stopAndRelease() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}