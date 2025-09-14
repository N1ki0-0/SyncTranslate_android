com.yourpackage/
├── data/
│   ├── local/
│   │   └── PreferencesManager.kt       # Хранит IP, порт и статус настройки
│   ├── remote/
│   │   ├── http/
│   │   │   ├── ApiConfig.kt            # Динамически формирует URL для HTTP
│   │   │   └── ApiService.kt           # Retrofit-интерфейс для загрузки файла
│   │   └── webrtc/
│   │       ├── SignalingCommand.kt     # Модели данных для JSON-сообщений
│   │       ├── SignalingClient.kt      # Клиент для WebSocket-сигналинга
│   │       ├── WebRtcClient.kt         # Основной класс для управления WebRTC
│   │       └── SdpObserverImpl.kt      # Вспомогательный класс-наблюдатель
│   └── repository/
│       └── AppRepositoryImpl.kt        # Реализация репозитория
├── di/
│   ├── AppModule.kt                   # Hilt-модуль для зависимостей приложения
│   └── NetworkModule.kt                 # Hilt-модуль для сетевых зависимостей
├── domain/
│   └── repository/
│       └── AppRepository.kt            # Интерфейс репозитория
├── presentation/
│   ├── navigation/
│   │   ├── AppNavigation.kt            # Логика навигации (NavHost)
│   │   └── AppRoutes.kt                # Константы маршрутов
│   ├── screens/
│   │   ├── ip_config/
│   │   │   ├── IpConfigScreen.kt
│   │   │   └── IpConfigViewModel.kt
│   │   ├── main/
│   │   │   ├── MainScreen.kt
│   │   │   ├── MainViewModel.kt        # Переименован из MainAppViewModel для краткости
│   │   │   └── RtcState.kt             # Класс состояний для UI
│   │   └── setup/
│   │       ├── SetupScreen.kt
│   │       └── SetupViewModel.kt
│   ├── util/
│   │   ├── AppLogger.kt                # Утилита для логгирования
│   │   ├── AudioSessionManager.kt      # Управляет AudioManager (фокус, динамики)
│   │   └── AudioRecorderUtil.kt        # Утилита для записи файла настройки
│   ├── MainActivity.kt                  # Главная Activity
└── ModulApp.kt                        # Класс Application