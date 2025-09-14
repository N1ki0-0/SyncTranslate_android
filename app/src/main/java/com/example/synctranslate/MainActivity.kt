package com.example.synctranslate

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.synctranslate.data.local.PreferencesManager
import com.example.synctranslate.presentation.AppNavigation
import com.example.synctranslate.presentation.main.MainAppScreen
import com.example.synctranslate.presentation.main.MainAppViewModel
import com.example.synctranslate.presentation.onboarding.SetupScreen
import com.example.synctranslate.presentation.onboarding.SetupViewModel
import com.example.synctranslate.ui.theme.SyncTranslateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Используй AppLogger из прошлого ответа для логирования
                // AppLogger.i("MainActivity", "Разрешение на запись аудио получено.")
            } else {
                // AppLogger.w("MainActivity", "Пользователь отклонил разрешение на запись аудио.")
            }
        }

    // Передаем эту функцию в наш навигатор
    private fun requestAudioPermission() {
        requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AppNavigation(
                    requestAudioPermission = ::requestAudioPermission,
                    preferencesManager = PreferencesManager(LocalContext.current)
                )
            }

        }
    }
}



@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SyncTranslateTheme {
        Greeting("Android")
    }
}