package com.example.synctranslate.data.remote.http

import com.example.synctranslate.data.local.PreferencesManager
import jakarta.inject.Inject


class ApiConfig @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    // Теперь это не константы, а вычисляемые свойства
    val baseUrl: String
        get() {
            val ip = preferencesManager.serverIp ?: "127.0.0.1"
            val port = preferencesManager.serverPort // Используем порт из настроек
            return "http://$ip:$port/"
        }

    val trainingEndpoint: String
        get() = "$baseUrl/train/voice"

    val wsUrl: String
        get() {
            val ip = preferencesManager.serverIp ?: "127.0.0.1"
            return "ws://$ip:8765"
        }
}