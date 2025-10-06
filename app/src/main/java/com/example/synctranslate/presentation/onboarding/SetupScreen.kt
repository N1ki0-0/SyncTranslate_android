package com.example.synctranslate.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel


@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    requestAudioPermission: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val recordingState by viewModel.recordingState.collectAsState()
    val isSetupCompleted by viewModel.isSetupCompleted.collectAsState()

    // Как только настройка завершена, переходим на главный экран
    LaunchedEffect(isSetupCompleted) {
        if (isSetupCompleted) {
            onSetupComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Настройка голоса", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Пожалуйста, запишите короткий аудиофрагмент для калибровки системы.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        when (recordingState) {
            RecordingState.IDLE -> {
                Button(onClick = {
                    if (viewModel.audioRecorderUtil.hasRecordAudioPermission()) {
                        viewModel.startRecording()
                    } else {
                        requestAudioPermission()
                    }
                }) {
                    Text("Начать запись")
                }
            }
            RecordingState.RECORDING -> {
                Text("Идет запись...")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.stopRecording() }) {
                    Text("Остановить запись")
                }
            }
            RecordingState.STOPPED -> {
                Text("Запись завершена. Готово к отправке.")
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = { viewModel.sendRecording() }) {
                        Text("Отправить на сервер")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { viewModel.retryRecording() }) {
                        Text("Повторить")
                    }
                }
            }
            RecordingState.SENDING -> {
                Text("Отправка на сервер...")
                CircularProgressIndicator()
            }
            RecordingState.SENT -> {
                Text("Настройка завершена!")
            }
            RecordingState.ERROR -> {
                Text("Произошла ошибка. Пожалуйста, попробуйте снова.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.retryRecording() }) {
                    Text("Повторить")
                }
            }
        }

        TextButton(
            onClick = { viewModel.skipSetup() },
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 32.dp)
        ) {
            Text("Пропустить этот шаг")
        }
    }
}