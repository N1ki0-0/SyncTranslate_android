package com.example.synctranslate.presentation.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.synctranslate.domain.repository.AudioRepository
import com.example.synctranslate.domain.useCase.CheckSetupCompletedUseCase
import com.example.synctranslate.domain.useCase.CompleteSetupUseCase
import com.example.synctranslate.util.AudioRecorderUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class RecordingState { IDLE, RECORDING, STOPPED, SENDING, SENT, ERROR }

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val checkSetupCompletedUseCase: CheckSetupCompletedUseCase,
    private val completeSetupUseCase: CompleteSetupUseCase,
    private val audioRepository: AudioRepository, // Инжектируем репозиторий
    val audioRecorderUtil: AudioRecorderUtil
) : ViewModel() {

    private val _isSetupCompleted = MutableStateFlow(false)
    val isSetupCompleted: StateFlow<Boolean> = _isSetupCompleted.asStateFlow()

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _recordedAudioFile = MutableStateFlow<File?>(null)
    val recordedAudioFile: StateFlow<File?> = _recordedAudioFile.asStateFlow()

    private val _showPermissionRationale = MutableStateFlow(false)
    val showPermissionRationale: StateFlow<Boolean> = _showPermissionRationale.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun checkIfSetupCompleted(): Boolean {
        val completed = checkSetupCompletedUseCase()
        _isSetupCompleted.value = completed
        return completed
    }

    fun onPermissionGranted() {
        _showPermissionRationale.value = false
        // Можно автоматически начать запись, если это было запрошено
    }

    fun onPermissionDenied(shouldShowRationale: Boolean) {
        if (shouldShowRationale) {
            _showPermissionRationale.value = true
        } else {
            // Пользователь выбрал "Don't ask again"
            _error.value = "Для записи нужен доступ к микрофону. Пожалуйста, предоставьте его в настройках приложения."
        }
    }


    fun startRecording() {
        if (!audioRecorderUtil.hasRecordAudioPermission()) {
            _error.value = "Нет разрешения на запись аудио."
            // Здесь MainActivity должна вызвать requestAudioPermission()
            return
        }
        viewModelScope.launch {
            _recordingState.value = RecordingState.RECORDING
            _error.value = null
            val file = audioRecorderUtil.startRecordingForSetup()
            if (file == null) {
                _recordingState.value = RecordingState.ERROR
                _error.value = "Не удалось начать запись."
            }
            // Файл будет доступен после остановки
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            val file = audioRecorderUtil.stopRecordingForSetup()
            if (file != null && file.exists()) {
                _recordedAudioFile.value = file
                _recordingState.value = RecordingState.STOPPED
                Log.d("SetupViewModel", "Recording stopped, file: ${file.absolutePath}")
            } else {
                _recordingState.value = RecordingState.ERROR
                _error.value = "Файл записи не найден или пуст после остановки."
                Log.e("SetupViewModel", "Recorded file is null or does not exist after stop.")
            }
        }
    }

    fun sendRecording() {
        val fileToSend = _recordedAudioFile.value
        if (fileToSend == null || !fileToSend.exists()) {
            _error.value = "Нет файла для отправки."
            _recordingState.value = RecordingState.ERROR
            return
        }

        viewModelScope.launch {
            _recordingState.value = RecordingState.SENDING
            try {
                val success = audioRepository.uploadSetupAudio(fileToSend) // Используем репозиторий
                if (success) {
                    _recordingState.value = RecordingState.SENT
                    completeSetupUseCase() // Отмечаем, что сетап пройден
                    _isSetupCompleted.value = true // Обновляем UI
                    fileToSend.delete() // Удаляем файл после успешной отправки
                    _recordedAudioFile.value = null
                } else {
                    _recordingState.value = RecordingState.ERROR
                    _error.value = "Ошибка отправки записи на сервер."
                }
            } catch (e: Exception) {
                _recordingState.value = RecordingState.ERROR
                _error.value = "Ошибка: ${e.message}"
                Log.e("SetupViewModel", "Error sending recording", e)
            }
        }
    }

    fun retryRecording() {
        _recordedAudioFile.value?.delete()
        _recordedAudioFile.value = null
        _recordingState.value = RecordingState.IDLE
        _error.value = null
    }


    fun markSetupAsCompleted() { // Используется если пользователь пропускает запись
        viewModelScope.launch {
            completeSetupUseCase()
            _isSetupCompleted.value = true
        }
    }
}