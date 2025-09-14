package com.example.synctranslate.domain.useCase

import com.example.synctranslate.domain.repository.SettingsRepository
import javax.inject.Inject

class CompleteSetupUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
){
    suspend operator fun invoke(completed: Boolean = true) = settingsRepository.setSetupCompleted(completed)

}