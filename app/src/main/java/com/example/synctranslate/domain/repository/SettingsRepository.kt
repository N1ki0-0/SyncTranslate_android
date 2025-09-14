package com.example.synctranslate.domain.repository

interface SettingsRepository {
    suspend fun isSetupCompleted(): Boolean
    suspend fun setSetupCompleted(completed: Boolean)
}