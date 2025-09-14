package com.example.synctranslate.presentation.ip

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun IpConfigScreen(
    onIpSaved: () -> Unit,
    viewModel: IpConfigViewModel = hiltViewModel()
) {
    val ipAddress by viewModel.ipAddress.collectAsState()
    val port by viewModel.port.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()

    // Как только настройки сохранятся, переходим дальше
    LaunchedEffect(isSaved) {
        if (isSaved) {
            onIpSaved()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Настройка сервера",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = ipAddress,
            onValueChange = { viewModel.onIpAddressChanged(it) },
            label = { Text("Введите IP-адрес сервера") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = port,
            onValueChange = { viewModel.onPortChanged(it) },
            label = { Text("Введите порт сервера") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.saveSettings() },
            enabled = ipAddress.isNotBlank() && port.toIntOrNull() != null
        ) {
            Text("Сохранить и продолжить")
        }
    }
}