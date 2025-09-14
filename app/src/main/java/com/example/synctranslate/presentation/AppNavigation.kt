package com.example.synctranslate.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.synctranslate.data.local.PreferencesManager
import com.example.synctranslate.presentation.ip.IpConfigScreen
import com.example.synctranslate.presentation.logs.LogsScreen
import com.example.synctranslate.presentation.main.MainAppScreen
import com.example.synctranslate.presentation.main.MainAppViewModel
import com.example.synctranslate.presentation.onboarding.SetupScreen
import com.example.synctranslate.presentation.onboarding.SetupViewModel


// 1. Определяем все наши маршруты в одном месте
object AppRoutes {
    const val IP_CONFIG = "ip_config"
    const val SETUP = "setup"
    const val MAIN = "main"
    const val LOGS = "logs"
}

// Определяем элементы для нижней навигационной панели
sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Main : BottomNavItem(AppRoutes.MAIN, Icons.Default.Home, "Главная")
    object Logs : BottomNavItem(AppRoutes.LOGS, Icons.Default.List, "Логи")
}

@Composable
fun AppNavigation(requestAudioPermission: () -> Unit,
                  preferencesManager: PreferencesManager ) {
    val navController = rememberNavController()
    val setupViewModel: SetupViewModel = hiltViewModel()

    // Используем State для определения, завершена ли проверка
    var isLoading by remember { mutableStateOf(true) }
    var isSetupComplete by remember { mutableStateOf(false) }

    // Асинхронно проверяем статус настройки при первом запуске
    LaunchedEffect(key1 = Unit) {
        isSetupComplete = setupViewModel.checkIfSetupCompleted()
        isLoading = false
    }

    // Пока идет проверка, можно показать экран загрузки
    if (isLoading) {
        // Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        //     CircularProgressIndicator()
        // }
        return // или показать сплэш-скрин
    }

    val startDestination = remember {
        if (preferencesManager.serverIp.isNullOrBlank()) {
            AppRoutes.IP_CONFIG
        } else if (!preferencesManager.isSetupCompleted) {
            AppRoutes.SETUP
        } else {
            AppRoutes.MAIN
        }
    }


    Scaffold(
        bottomBar = {
            // Показываем нижнюю панель только если мы НЕ на экране настройки
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute != AppRoutes.SETUP) {
                AppBottomNavigation(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(AppRoutes.IP_CONFIG) {
                IpConfigScreen(
                    onIpSaved = {
                        // После сохранения IP переходим на экран настройки голоса
                        navController.navigate(AppRoutes.SETUP) {
                            popUpTo(AppRoutes.IP_CONFIG) { inclusive = true }
                        }
                    }
                )
            }

            composable(AppRoutes.SETUP) {
                SetupScreen(
                    viewModel = setupViewModel,
                    onSetupComplete = {
                        // Важный момент: переходим на главный экран и удаляем
                        // экран настройки из истории, чтобы на него нельзя было вернуться кнопкой "назад"
                        navController.navigate(AppRoutes.MAIN) {
                            popUpTo(AppRoutes.SETUP) {
                                inclusive = true
                            }
                        }
                    },
                    requestAudioPermission = requestAudioPermission
                )
            }

            composable(AppRoutes.MAIN) {
                val mainAppViewModel: MainAppViewModel = hiltViewModel()
                MainAppScreen(
                    navController = navController, // <-- ПЕРЕДАЕМ NAVCONTROLLER
                    viewModel = mainAppViewModel,
                    requestAudioPermission = requestAudioPermission
                )
            }

            composable(AppRoutes.LOGS) {
                LogsScreen() // Экран с логами из прошлого ответа
            }
        }
    }
}

@Composable
fun AppBottomNavigation(navController: NavHostController) {
    val items = listOf(BottomNavItem.Main, BottomNavItem.Logs)

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Это стандартный код, чтобы не создавать копии экранов при клике на вкладки
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}