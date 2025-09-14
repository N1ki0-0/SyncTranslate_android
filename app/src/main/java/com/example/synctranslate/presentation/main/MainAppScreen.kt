package com.example.synctranslate.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.synctranslate.presentation.AppRoutes


@Composable
fun MainAppScreen(
    navController: NavController,
    viewModel: MainAppViewModel,
    requestAudioPermission: () -> Unit
) {
    val rtcState by viewModel.rtcState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Основной контент по центру
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val statusText = when (rtcState) {
                RtcState.IDLE -> "Готово к подключению"
                RtcState.SIGNALING -> "Подключение к серверу..."
                RtcState.CREATING -> "Установка соединения..."
                RtcState.CONNECTED -> "Соединение активно"
                RtcState.FAILED -> "Ошибка соединения"
                RtcState.CLOSED -> "Отключено"
            }
            Text(statusText, style = MaterialTheme.typography.headlineSmall)

            if (rtcState == RtcState.SIGNALING || rtcState == RtcState.CREATING) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }

            if (rtcState == RtcState.CONNECTED) {
                Text(
                    "Идет обмен аудиоданными...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (rtcState == RtcState.CONNECTED || rtcState == RtcState.SIGNALING || rtcState == RtcState.CREATING) {
                Button(
                    onClick = { viewModel.stopCommunication() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text("Остановить")
                }
            } else {
                Button(
                    onClick = {
                        if (viewModel.hasAudioPermission()) {
                            viewModel.startCommunication()
                        } else {
                            requestAudioPermission()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text("Начать")
                }
            }
        }

        // Кнопки повторной настройки внизу
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Повторная настройка",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedButton(
                onClick = { navController.navigate(AppRoutes.SETUP) },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Перезаписать голос")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { navController.navigate(AppRoutes.IP_CONFIG) },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Изменить IP-адрес сервера")
            }
        }
    }
}