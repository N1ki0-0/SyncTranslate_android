package com.example.synctranslate.data.repository

import com.example.synctranslate.data.local.PreferencesManager
import com.example.synctranslate.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val preferencesManager: PreferencesManager
) : SettingsRepository {
    override suspend fun isSetupCompleted(): Boolean = withContext(Dispatchers.IO) {
        preferencesManager.isSetupCompleted
    }

    override suspend fun setSetupCompleted(completed: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.isSetupCompleted = completed
    }
}