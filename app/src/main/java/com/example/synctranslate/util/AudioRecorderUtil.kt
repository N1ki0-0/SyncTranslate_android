package com.example.synctranslate.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import javax.inject.Inject
import android.media.AudioFormat
import android.media.AudioRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Singleton
class AudioRecorderUtil @Inject constructor(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var setupAudioFile: File? = null
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val audioDataFlow = MutableSharedFlow<ByteArray>()

    private val sampleRate = 48000 // Используем 48000 Гц, как в Python-скрипте
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun getAudioStream(): Flow<ByteArray> = audioDataFlow

    @SuppressLint("MissingPermission")
    fun startStreamingAudio(scope: CoroutineScope) {
        if (recordingJob?.isActive == true) return

        val bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (bufferSizeInBytes == AudioRecord.ERROR_BAD_VALUE) return

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSizeInBytes
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) return

        audioRecord?.startRecording()
        recordingJob = scope.launch(Dispatchers.IO) {
            val audioBuffer = ByteArray(bufferSizeInBytes)
            while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val readSize = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                AppLogger.d("AudioFlow", "1. Микрофон: Прочитано $readSize байт")
                if (readSize > 0) {
                    // Отправляем сырые байты, как они есть
                    audioDataFlow.emit(audioBuffer.copyOfRange(0, readSize))
                }
            }
        }
    }

    fun stopStreamingAudio() {
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    fun stopRecordingForSetup(): File? {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            AppLogger.e("AudioRecorderUtil", "Error stopping MediaRecorder $e",)
        } finally {
            mediaRecorder = null
        }
        return setupAudioFile?.takeIf { it.exists() && it.length() > 0 }
    }

    // Add the missing permission check function
    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun releaseMediaRecorder() {
        mediaRecorder?.reset()
        mediaRecorder?.release()
        mediaRecorder = null
        setupAudioFile?.delete()
        setupAudioFile = null
    }

    @SuppressLint("MissingPermission")
    fun startRecordingForSetup(): File? {
        if (!hasRecordAudioPermission()) {
            AppLogger.e("AudioRecorderUtil", "Record audio permission not granted for setup.")
            return null
        }
        setupAudioFile = File(context.cacheDir, "setup_audio.wav")
        mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setAudioSamplingRate(sampleRate)
            setOutputFile(setupAudioFile?.absolutePath)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                releaseMediaRecorder()
                return null
            }
        }
        return setupAudioFile
    }
}