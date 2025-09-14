package com.example.synctranslate.di

import android.content.Context
import com.example.synctranslate.util.AudioPlayerUtil
import com.example.synctranslate.util.AudioRecorderUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideAudioRecorderUtil(@ApplicationContext context: Context): AudioRecorderUtil {
        return AudioRecorderUtil(context)
    }


}