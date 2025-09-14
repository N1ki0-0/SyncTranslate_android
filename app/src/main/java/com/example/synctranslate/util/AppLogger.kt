package com.example.synctranslate.util

import android.util.Log
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Уровни логов с цветами для UI
enum class LogLevel(val color: Color) {
    DEBUG(Color.Gray),
    INFO(Color.Blue),
    WARN(Color(0xFFFFA500)), // Orange
    ERROR(Color.Red)
}

// Структура для одной записи в логе
data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String
) {
    fun formattedTimestamp(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

// Singleton для управления логами
object AppLogger {

    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries = _logEntries.asStateFlow()

    private const val MAX_LOG_ENTRIES = 500 // Храним последние 500 записей

    fun log(level: LogLevel, tag: String, message: String) {
        val newEntry = LogEntry(level = level, tag = tag, message = message)

        // Обновляем список, сохраняя только последние MAX_LOG_ENTRIES записей
        val updatedList = (_logEntries.value + newEntry).takeLast(MAX_LOG_ENTRIES)
        _logEntries.value = updatedList

        // Также дублируем в системный Logcat для удобства отладки в IDE
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }
    }

    // Удобные функции-ярлыки
    fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
    fun e(tag: String, message: String) = log(LogLevel.ERROR, tag, message)

    fun clear() {
        _logEntries.value = emptyList()
    }
}