package com.example.synctranslate.presentation.main

enum class RtcState {
    IDLE,        // Ничего не происходит
    SIGNALING,   // Подключаемся к WebSocket
    CREATING,    // Создаем Offer, ждем Answer
    CONNECTED,   // Медиа-соединение установлено
    CLOSED,      // Закрыто
    FAILED       // Ошибка
}