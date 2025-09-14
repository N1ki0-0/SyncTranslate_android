package com.example.synctranslate.AI.data.audio

interface AudioRecorder {
    fun startRecording(format: AudioFormat)
    fun stopRecording(): String // возвращает путь к файлу
    fun isRecording(): Boolean
}

enum class AudioFormat(val extension: String) {
    MP3("mp3"),
    WAV("wav")
}