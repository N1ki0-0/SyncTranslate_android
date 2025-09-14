package com.example.synctranslate.di

import android.content.Context
import com.example.synctranslate.data.local.PreferencesManager
import com.example.synctranslate.data.repository.AudioRepositoryImpl
import com.example.synctranslate.data.repository.SettingsRepositoryImpl
import com.example.synctranslate.domain.repository.AudioRepository
import com.example.synctranslate.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
    fun provideSettingsRepository(preferencesManager: PreferencesManager): SettingsRepository {
        return SettingsRepositoryImpl(preferencesManager)
    }

    @Provides
    @Singleton
    fun provideAudioRepository(audioRepositoryImpl: AudioRepositoryImpl): AudioRepository {
        return audioRepositoryImpl
    }
}