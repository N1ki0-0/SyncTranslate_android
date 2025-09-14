package com.example.synctranslate.AI.data.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class AudioPreprocessor(private val context: Context) {
    companion object {
        private const val TAG = "AudioPreprocessor"
        private const val TIMEOUT_US = 10000L
    }

    fun convertToWav(inputPath: String): String {
        val outputFileName = "converted_${System.currentTimeMillis()}.wav"
        val outputFile = File(context.filesDir, outputFileName)

        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(inputPath)

            // Find the audio track
            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex == -1) {
                throw RuntimeException("No audio track found in the input file")
            }

            // Select the audio track
            extractor.selectTrack(audioTrackIndex)
            val inputFormat = extractor.getTrackFormat(audioTrackIndex)

            // Create output format for PCM
            val outputFormat = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_RAW,
                16000, // Sample rate
                1 // Channel count (mono)
            )
            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, 256000)

            // Create decoder
            val decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME) ?: "")
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            val outputStream = FileOutputStream(outputFile)

            // Write WAV header
            writeWavHeader(outputStream, 16000, 1)

            var isEOS = false
            var outputBufferIndex: Int
            var inputBufferIndex: Int

            while (!isEOS) {
                inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                    val sampleSize = extractor.readSampleData(inputBuffer!!, 0)

                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEOS = true
                    } else {
                        decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.get(chunk)
                        outputStream.write(chunk)
                    }
                    decoder.releaseOutputBuffer(outputBufferIndex, false)
                }
            }

            // Cleanup
            extractor.release()
            decoder.stop()
            decoder.release()
            outputStream.close()

            return outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error during audio conversion", e)
            throw RuntimeException("Failed to convert audio: ${e.message}", e)
        }
    }

    private fun writeWavHeader(outputStream: FileOutputStream, sampleRate: Int, channels: Int) {
        val header = ByteArray(44)
        val dataSize = 0 // Will be updated later
        val byteRate = sampleRate * channels * 2

        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        // File size
        header[4] = (dataSize and 0xff).toByte()
        header[5] = (dataSize shr 8 and 0xff).toByte()
        header[6] = (dataSize shr 16 and 0xff).toByte()
        header[7] = (dataSize shr 24 and 0xff).toByte()

        // WAVE header
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // fmt chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        // Chunk size
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0

        // Audio format (1 for PCM)
        header[20] = 1
        header[21] = 0

        // Number of channels
        header[22] = channels.toByte()
        header[23] = 0

        // Sample rate
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()

        // Byte rate
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()

        // Block align
        header[32] = (channels * 2).toByte()
        header[33] = 0

        // Bits per sample
        header[34] = 16
        header[35] = 0

        // data chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        // Data size
        header[40] = (dataSize and 0xff).toByte()
        header[41] = (dataSize shr 8 and 0xff).toByte()
        header[42] = (dataSize shr 16 and 0xff).toByte()
        header[43] = (dataSize shr 24 and 0xff).toByte()

        outputStream.write(header)
    }
}