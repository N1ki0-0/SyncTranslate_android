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
import javax.inject.Singleton

@Singleton
class AudioRecorderUtil @Inject constructor(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var setupAudioFile: File? = null
    private val sampleRate = 16000

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
}