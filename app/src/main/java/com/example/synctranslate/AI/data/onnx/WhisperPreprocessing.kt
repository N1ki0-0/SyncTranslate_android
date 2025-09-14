package com.example.synctranslate.AI.data.onnx

import com.example.synctranslate.AI.data.audio.WavUtils
import java.io.File
import java.nio.FloatBuffer
import kotlin.math.*

object WhisperPreprocessing {

    fun audioToMel(file: File): Array<Array<FloatArray>> {
        val pcmData = WavUtils.decodeWavToFloatArray(file)
        val melMatrix: List<FloatArray> = generateMelSpectrogram(pcmData, 16000)

        // Преобразуем [80][3000] → [1][80][3000]
        val output = Array(1) { Array(80) { FloatArray(3000){0f} } }
        for (i in 0 until 80) {
            for (j in 0 until melMatrix[i].size.coerceAtMost(3000)) {
                output[0][i][j] = melMatrix[i][j]
            }
        }
        return output
    }

    private fun generateMelSpectrogram(audio: FloatArray, sampleRate: Int): List<FloatArray> {
        val frameSize = 400  // 25ms @16kHz
        val hopLength = 160  // 10ms
        val fftSize = 512
        val melBands = 80

        val frames = audio.toList().windowed(frameSize, hopLength, partialWindows = false)
        val melSpec = mutableListOf<FloatArray>()

        for (frame in frames) {
            val windowed = hannWindow(frame)
            val spectrum = stft(windowed, fftSize)
            val mel = melFilter(spectrum, sampleRate, fftSize, melBands)
            melSpec.add(mel)
        }

        return melSpec
    }

    private fun hannWindow(frame: List<Float>): FloatArray {
        val N = frame.size
        return FloatArray(N) { i ->
            (0.5f - 0.5f * cos(2.0 * Math.PI * i / N).toFloat()) * frame[i]
        }
    }

    private fun stft(frame: FloatArray, fftSize: Int): FloatArray {
        val re = frame.copyOf(fftSize)
        val im = FloatArray(fftSize)
        fft(re, im)

        return FloatArray(fftSize / 2) { i ->
            sqrt(re[i].pow(2) + im[i].pow(2))
        }
    }

    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        val levels = (log2(n.toFloat())).toInt()

        val cosTable = FloatArray(n / 2)
        val sinTable = FloatArray(n / 2)
        for (i in 0 until n / 2) {
            cosTable[i] = cos(2 * PI * i / n).toFloat()
            sinTable[i] = sin(2 * PI * i / n).toFloat()
        }

        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j >= bit) {
                j -= bit
                bit = bit shr 1
            }
            j += bit
            if (i < j) {
                val tempRe = re[i]; re[i] = re[j]; re[j] = tempRe
                val tempIm = im[i]; im[i] = im[j]; im[j] = tempIm
            }
        }

        var size = 2
        while (size <= n) {
            val halfSize = size / 2
            val tableStep = n / size
            for (i in 0 until n step size) {
                var k = 0
                for (j in 0 until halfSize) {
                    val tRe = re[i + j + halfSize] * cosTable[k] + im[i + j + halfSize] * sinTable[k]
                    val tIm = -re[i + j + halfSize] * sinTable[k] + im[i + j + halfSize] * cosTable[k]

                    re[i + j + halfSize] = re[i + j] - tRe
                    im[i + j + halfSize] = im[i + j] - tIm
                    re[i + j] += tRe
                    im[i + j] += tIm

                    k += tableStep
                }
            }
            size *= 2
        }
    }

    private fun melFilter(spectrum: FloatArray, sampleRate: Int, fftSize: Int, melBands: Int): FloatArray {
        // Простейшая заглушка для примера — используем log мощности спектра
        return FloatArray(melBands) { i ->
            ln((spectrum.getOrNull(i * spectrum.size / melBands) ?: 1e-6f))
        }
    }
}