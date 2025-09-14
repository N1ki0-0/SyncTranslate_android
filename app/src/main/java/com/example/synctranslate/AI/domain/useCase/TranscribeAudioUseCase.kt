package com.example.synctranslate.AI.domain.useCase

import com.example.synctranslate.AI.domain.model.TranscriptionResult


interface TranscribeAudioUseCase {
    suspend fun invoke(mp3Path: String): TranscriptionResult
}