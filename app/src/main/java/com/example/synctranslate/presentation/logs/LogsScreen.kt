package com.example.synctranslate.presentation.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.synctranslate.util.AppLogger
import com.example.synctranslate.util.LogEntry


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen() {
    val logs by AppLogger.logEntries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Логи приложения") },
                actions = {
                    IconButton(onClick = { AppLogger.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Очистить логи")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Отображаем логи в обратном порядке, чтобы новые были сверху
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            reverseLayout = true // Новые элементы будут сверху
        ) {
            items(logs) { log ->
                LogItem(log = log)
            }
        }
    }
}

@Composable
fun LogItem(log: LogEntry) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "${log.formattedTimestamp()} ",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Text(
            text = "[${log.tag}]",
            color = log.level.color,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = log.message,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontFamily = FontFamily.Default
        )
    }
}