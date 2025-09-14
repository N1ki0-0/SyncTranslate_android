package com.example.synctranslate.util

import android.content.Context
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Этот класс теперь отвечает только за управление динамиком
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun setSpeakerphoneOn(isOn: Boolean) {
        audioManager.isSpeakerphoneOn = isOn
        AppLogger.i("AudioSessionManager", "Громкая связь теперь: ${if (isOn) "ВКЛ" else "ВЫКЛ"}")
    }
}