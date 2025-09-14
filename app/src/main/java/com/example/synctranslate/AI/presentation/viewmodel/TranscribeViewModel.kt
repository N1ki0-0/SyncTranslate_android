package com.example.synctranslate.AI.presentation.viewmodel

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.synctranslate.AI.data.audio.AudioFormat
import com.example.synctranslate.AI.data.audio.AudioPlayer
import com.example.synctranslate.AI.data.audio.AudioRecorder
import com.example.synctranslate.AI.data.onnx.WhisperTranscriber
import com.example.synctranslate.AI.presentation.viewmodel.state.TranscribeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject


@HiltViewModel
class TranscribeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recorder: AudioRecorder,
    private val transcriber: WhisperTranscriber,
    private val player: AudioPlayer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranscribeUiState())
    val uiState: StateFlow<TranscribeUiState> = _uiState

//    private val tts: TextToSpeech = TextToSpeech(context) { status ->
//        if (status == TextToSpeech.SUCCESS) {
//            tts.language = Locale.ENGLISH
//        }
//    }

//    fun speakTranslatedText() {
//        val text = _uiState.value.transcribedText
//        if (!text.isNullOrEmpty()) {
//            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
//        }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        tts.shutdown()
//    }

    fun startRecording(format: AudioFormat) {
        recorder.startRecording(format)
        _uiState.value = _uiState.value.copy(isRecording = true, transcribedText = null, audioPath = null)
    }

    fun stopRecording() {
        val filePath = recorder.stopRecording()
        _uiState.value = _uiState.value.copy(isRecording = false, audioPath = filePath)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val resultText = transcriber.transcribe(filePath)
                _uiState.value = _uiState.value.copy(transcribedText = resultText)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(transcribedText = "Ошибка: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun playAudio() {
        _uiState.value.audioPath?.let { player.play(it) }
    }

    fun stopAudio() {
        player.stop()
    }
}