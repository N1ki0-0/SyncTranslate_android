package com.example.synctranslate.AI.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class AudioRecorderImpl @Inject constructor(
    private val context: Context
) : AudioRecorder {

    private var recorder: MediaRecorder? = null
    private var filePath: String = ""
    private var isRecordingNow = false

    override fun startRecording(format: AudioFormat) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "AUDIO_${timeStamp}.${format.extension}"
        val storageDir = File(context.filesDir, "recordings")

        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val outputFile = File(storageDir, fileName)
        filePath = outputFile.absolutePath

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(
                if (format == AudioFormat.MP3) MediaRecorder.OutputFormat.MPEG_4
                else MediaRecorder.OutputFormat.THREE_GPP
            )
            setAudioEncoder(
                if (format == AudioFormat.MP3) MediaRecorder.AudioEncoder.AAC
                else MediaRecorder.AudioEncoder.AMR_NB
            )
            setOutputFile(filePath)
            prepare()
            start()
            isRecordingNow = true
        }
    }

    override fun stopRecording(): String {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        isRecordingNow = false
        return filePath
    }

    override fun isRecording(): Boolean = isRecordingNow
}