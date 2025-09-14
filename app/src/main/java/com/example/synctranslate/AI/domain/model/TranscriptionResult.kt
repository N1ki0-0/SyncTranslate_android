package com.example.synctranslate.AI.domain.model

data class TranscriptionResult(
    val text: String,
    val audioPath: String? = null
)
