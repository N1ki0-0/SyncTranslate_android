package com.example.synctranslate.AI.presentation.viewmodel.state

data class TranscribeUiState(
    val isRecording: Boolean = false,
    val isLoading: Boolean = false,
    val audioPath: String? = null,
    val transcribedText: String? = null
)