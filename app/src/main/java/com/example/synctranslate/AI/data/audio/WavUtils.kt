package com.example.synctranslate.AI.data.audio

import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavUtils {

    fun decodeWavToFloatArray(wavFile: File): FloatArray {
        val inputStream = FileInputStream(wavFile)
        val header = ByteArray(44) // стандартный WAV заголовок
        inputStream.read(header)

        val riff = String(header.copyOfRange(0, 4))
        require(riff == "RIFF") { "Неверный формат WAV" }

        val byteArray = inputStream.readBytes()
        val buffer = ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN)

        val samples = FloatArray(byteArray.size / 2)
        for (i in samples.indices) {
            samples[i] = buffer.short / 32768.0f // нормализация PCM
        }

        inputStream.close()
        return samples
    }
}