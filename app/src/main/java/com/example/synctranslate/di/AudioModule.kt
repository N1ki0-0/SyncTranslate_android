package com.example.synctranslate.di

import android.content.Context
import com.example.synctranslate.data.repository.AppRepositoryImpl
import com.example.synctranslate.domain.repository.AppRepository
import com.example.synctranslate.util.AudioRecorderUtil
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindAppRepository(impl: AppRepositoryImpl): AppRepository


    companion object {
        @Provides
        @Singleton
        fun provideAudioRecorderUtil(@ApplicationContext context: Context): AudioRecorderUtil {
            return AudioRecorderUtil(context)
        }
    }
}