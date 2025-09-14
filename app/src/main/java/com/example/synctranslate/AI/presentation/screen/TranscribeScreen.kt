package com.example.synctranslate.AI.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.synctranslate.AI.data.audio.AudioFormat
import com.example.synctranslate.AI.presentation.viewmodel.TranscribeViewModel


@Composable
fun TranscribeScreen(viewModel: TranscribeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (state.isRecording) {
            Button(onClick = { viewModel.stopRecording() }) {
                Text("🛑 Остановить запись")
            }
        } else {
            Row {
                Button(onClick = { viewModel.startRecording(AudioFormat.WAV) }) {
                    Text("🎙 Запись WAV")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.startRecording(AudioFormat.MP3) }) {
                    Text("🎙 Запись MP3")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when {
            state.isLoading -> CircularProgressIndicator()
            state.transcribedText != null -> {
                Text("📝 Текст: ${state.transcribedText}")
                Spacer(modifier = Modifier.height(16.dp))
                state.audioPath?.let {
                    Row {
                        Button(onClick = { viewModel.playAudio() }) {
                            Text("▶️ Слушать запись")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { viewModel.stopAudio() }) {
                            Text("⏹ Стоп")
                        }
                    }
                }
            }
        }
    }
}