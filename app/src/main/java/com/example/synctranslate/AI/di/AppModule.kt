package com.example.synctranslate.AI.di

import android.content.Context
import com.example.synctranslate.AI.data.audio.AudioPlayer
import com.example.synctranslate.AI.data.audio.AudioPlayerImpl
import com.example.synctranslate.AI.data.audio.AudioRecorder
import com.example.synctranslate.AI.data.audio.AudioRecorderImpl
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
    fun provideRecorder(
        @ApplicationContext context: Context
    ): AudioRecorder = AudioRecorderImpl(context)

    @Provides
    @Singleton
    fun providePlayer(): AudioPlayer = AudioPlayerImpl()
}