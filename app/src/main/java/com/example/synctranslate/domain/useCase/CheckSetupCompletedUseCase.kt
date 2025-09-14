package com.example.synctranslate.domain.useCase

import com.example.synctranslate.domain.repository.SettingsRepository
import javax.inject.Inject

class CheckSetupCompletedUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Boolean = settingsRepository.isSetupCompleted()
}