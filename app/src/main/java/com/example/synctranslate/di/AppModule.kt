package com.example.synctranslate.di

import android.content.Context
import com.example.synctranslate.data.local.PreferencesManager
import com.example.synctranslate.data.remote.http.ApiService
import com.example.synctranslate.data.remote.webrtc.SignalingClient
import com.example.synctranslate.data.remote.webrtc.WebRtcClient
import com.example.synctranslate.data.repository.AppRepositoryImpl
import com.example.synctranslate.data.repository.SettingsRepositoryImpl
import com.example.synctranslate.domain.repository.AppRepository
import com.example.synctranslate.domain.repository.SettingsRepository
import com.example.synctranslate.util.AudioRecorderUtil
import com.example.synctranslate.util.AudioSessionManager
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideAudioSessionManager(@ApplicationContext context: Context): AudioSessionManager {
        return AudioSessionManager(context)
    }



    @Provides
    @Singleton
    fun provideWebRtcClient(
        @ApplicationContext context: Context,
        gson: Gson
    ): WebRtcClient {
        return WebRtcClient(context, gson)
    }

    // --- Репозитории (здесь мы исправляем ошибки MissingBinding) ---

    @Provides
    @Singleton
    fun provideSettingsRepository(preferencesManager: PreferencesManager): SettingsRepository {
        // Этот "рецепт" говорит: когда кто-то просит SettingsRepository,
        // создай и верни SettingsRepositoryImpl
        return SettingsRepositoryImpl(preferencesManager)
    }


}