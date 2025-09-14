package com.example.synctranslate.AI.data.onnx

interface WhisperTranscriber {
    suspend fun transcribe(audioFilePath: String): String
}